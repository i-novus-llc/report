package net.n2oapp.cloud.report.service.rest;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.extern.slf4j.Slf4j;
import net.n2oapp.cloud.report.api.ReportService;
import net.n2oapp.cloud.report.service.TestApplication;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import javax.ws.rs.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static net.n2oapp.cloud.report.service.rest.ReportServiceRestImplTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = {"classpath:application-test.properties"},
        properties = {
                "report-service.s3.endpoint=http://localhost:9991",
                "report-service.s3.secret-key=test_user",
                "report-service.s3.access-key=test_user",
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

    private static GenericContainer<?> minio;
    private static final int CONCURRENT_USERS = 200;
    private static final int TEST_TIMEOUT_SECONDS = 30;

    @Autowired
    private ReportService reportService;

    @DynamicPropertySource
    static void setupMinio(DynamicPropertyRegistry registry) {
        minio = new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
                .withExposedPorts(9000)
                .withEnv("MINIO_ROOT_USER", "test_user")
                .withEnv("MINIO_ROOT_PASSWORD", "test_user")
                .withCommand("server /data");
        minio.start();
        var endpoint = "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
        registry.add("report-service.s3.endpoint", () -> endpoint);
    }

    @BeforeEach
    void setupDb() throws SQLException, IOException {
        var conn = DriverManager.getConnection("jdbc:h2:mem:in_memory_db;DB_CLOSE_DELAY=-1",
                "test_user",
                "test_user");
        try (Statement stmt = conn.createStatement()) {
            var is = getClass().getClassLoader().getResourceAsStream("init.sql");
            var sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            stmt.execute(sql);
        }
        conn.close();
    }

    @Test
    void loadTest() throws IOException {
        //Компиляция шаблона и загрузка в тест контейнер MinIo
        testCompile(EXAMPLE_TEMPLATE_FILE_NAME, reportService);

        var uriInfo = mockUriInfo();
        var startLatch = new CountDownLatch(1);
        var successFinishingLatch = new CountDownLatch(CONCURRENT_USERS);
        var executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        var errorCount = new AtomicInteger(0);

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    //Генерация отчета по шаблону из MinIo
                    try (Response response = reportService.generateReport(EXAMPLE_TEMPLATE_FILE_NAME, "xlsx", uriInfo)) {
                        assertEquals(200, response.getStatus());
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

    private UriInfo mockUriInfo() {
        var uriInfo = Mockito.mock(UriInfo.class);
        MultivaluedMap<String, String> queryParamMap = new MultivaluedHashMap<>();
        queryParamMap.put("REPORT_DATASOURCE_NAME", List.of(IN_MEMORY_DB_NAME));
        queryParamMap.put("title", List.of("abba"));
        when(uriInfo.getQueryParameters()).thenReturn(queryParamMap);
        return uriInfo;
    }
}
