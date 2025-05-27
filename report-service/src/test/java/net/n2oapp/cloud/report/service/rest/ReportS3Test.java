package net.n2oapp.cloud.report.service.rest;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import lombok.extern.slf4j.Slf4j;
import net.n2oapp.cloud.report.api.ReportService;
import net.n2oapp.cloud.report.service.TestApplication;
import net.n2oapp.cloud.report.service.filestorage.FileStorage;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.util.JRLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.ws.rs.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static net.n2oapp.cloud.report.service.rest.ReportServiceRestImplTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = {"classpath:application-test.properties"},
        properties = {
                "report-service.s3.endpoint=\"http://test-endpoint",
                "report.data.values[0].driver=org.h2.Driver",
                "report.data.values[0].name=in_memory_db",
                "report.data.values[0].url=jdbc:h2:mem:in_memory_db",
                "report.data.values[0].username=test_user",
                "report.data.values[0].password=test_user"
        }
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
class ReportS3Test {

    private static final int CONCURRENT_USERS = 1000;
    private static final int TEST_TIMEOUT_SECONDS = 60;
    private static final int S3_RESPONSE_TIMEOUT_MS = 3000;

    @Autowired
    private ReportService reportService;
    @Autowired
    private AmazonS3 amazonS3;
    @Value("${fileStorage.root}")
    private String fileStorageRoot;

    @BeforeEach
    void setup() throws SQLException, IOException {
        var conn = DriverManager.getConnection("jdbc:h2:mem:in_memory_db;DB_CLOSE_DELAY=-1", "test_user", "test_user");
        try (Statement stmt = conn.createStatement()) {
            var is = getClass().getClassLoader().getResourceAsStream("init.sql");
            var sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            stmt.execute(sql);
        }
        conn.close();
    }

    @AfterEach
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void removeFileStorageRoot() throws IOException {
        Files.walk(Paths.get(fileStorageRoot))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    void loadTest() throws IOException, JRException {
        var fs = mockS3Storage();

        testCompile(EXAMPLE_TEMPLATE_FILE_NAME, reportService);

        var uriInfo = Mockito.mock(UriInfo.class);
        MultivaluedMap<String, String> queryParamMap = new MultivaluedHashMap<>();
        queryParamMap.put("REPORT_DATASOURCE_NAME", List.of(IN_MEMORY_DB_NAME));
        queryParamMap.put("title", List.of("abba"));
        when(uriInfo.getQueryParameters()).thenReturn(queryParamMap);

        var startLatch = new CountDownLatch(1);
        var successFinishingLatch = new CountDownLatch(CONCURRENT_USERS);
        var executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        var successCount = new AtomicInteger(0);
        var errorCount = new AtomicInteger(0);

        var jasperReport = getJasperReport(fs);
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    try (var staticMock = Mockito.mockStatic(JRLoader.class)) {
                        //мок на возвращение реального файла отчета
                        staticMock.when(() -> JRLoader.loadObject(any(InputStream.class)))
                                .thenAnswer(invocation -> jasperReport);
                        //генерация отчета
                        testGenerate("xlsx", uriInfo, reportService);
                        try (Response response = reportService.generateReport(EXAMPLE_TEMPLATE_FILE_NAME, "xlsx", uriInfo)) {
                            assertEquals(200, response.getStatus());
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    successFinishingLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        assertDoesNotThrow(() -> successFinishingLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(0, errorCount.get());
    }

    private FileStorage mockS3Storage() {
        // Подменяем amazonS3.putObject() на загрузку файла в память
        var fs = new FileStorage();
        fs.setRoot(fileStorageRoot);
        when(amazonS3.putObject(any(), any(), any(), any())).thenAnswer((invocation) -> {
            var compiledFileStream = invocation.getArgument(2, InputStream.class);
            fs.saveContentWithFullPath(compiledFileStream, EXAMPLE_TEMPLATE_FILE_NAME + ".jasper");
            return null;
        });

        // mock amazonS3.getObject()
        var contentStr = "testcontent";
        var s3Object = new S3Object();
        s3Object.setObjectContent(new ByteArrayInputStream(contentStr.getBytes()));
        when(amazonS3.getObject((String) any(), any())).thenAnswer(invocation -> {
            var randomDelay = ThreadLocalRandom.current().nextInt(0, S3_RESPONSE_TIMEOUT_MS + 1);
            Thread.sleep(randomDelay);
            return s3Object;
        });
        return fs;
    }

    private Object getJasperReport(FileStorage fs) throws JRException {
        var content = fs.getContent(EXAMPLE_TEMPLATE_FILE_NAME + ".jasper");
        return JRLoader.loadObject(content);
    }
}
