package net.n2oapp.cloud.report.service.rest;

import junit.framework.TestCase;
import net.n2oapp.cloud.report.api.ReportService;
import net.n2oapp.cloud.report.service.ReportApplication;
import net.n2oapp.platform.test.autoconfigure.DefinePort;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import javax.ws.rs.core.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = ReportApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations = {"classpath:application-test.properties"})
@DefinePort
public class ReportServiceRestImplTest extends TestCase {

    private static final String PATH = "/net/n2oapp/report/service/rest/";
    private static final String MASTER_TEMPLATE_FILE_NAME = "masterReportTemplate";
    private static final String DETAIL_TEMPLATE_FILE_NAME = "detailReportTemplate";
    private static final String EXAMPLE_TEMPLATE_FILE_NAME = "exampleReportTemplate";
    private static final String IN_MEMORY_DB_NAME = "testdb";

    @Value("${fileStorage.root}")
    private String fileStorageRoot;

    @Autowired
    private ReportService reportService;

    @Autowired
    private Map<String, DataSource> dataSourcesWithName;

    @Test
    public void testGetReportFromInMemoryDb() throws Exception {
        testCompile(EXAMPLE_TEMPLATE_FILE_NAME);

        DataSource dataSource = dataSourcesWithName.get(IN_MEMORY_DB_NAME);
        initData(dataSource);

        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        MultivaluedMap queryParamMap = new MultivaluedHashMap();
        queryParamMap.put("REPORT_DATASOURCE_NAME", List.of(IN_MEMORY_DB_NAME));
        Mockito.when(uriInfo.getQueryParameters()).thenReturn(queryParamMap);

        try (Response response = reportService.generateReport(EXAMPLE_TEMPLATE_FILE_NAME, "csv", uriInfo)) {
            assertEquals(response.getStatus(), 200);
            assertTrue(response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION).get(0).toString().contains(EXAMPLE_TEMPLATE_FILE_NAME + "." + "csv"));
            compareContents(response);
        }

    }

    private void compareContents(Response response) {
        ByteArrayInputStream inputStream = (ByteArrayInputStream) response.getEntity();
        String[] employees = new String(inputStream.readAllBytes()).split("\n");
        String firstEmployee = employees[0];
        String firstEmployeeName = firstEmployee.substring(employees[0].indexOf("Michael"));
        assertEquals("Michael", firstEmployeeName);
        assertEquals("25", employees[1]);
        assertEquals("1000", employees[2]);
        assertEquals("John", employees[3]);
        assertEquals("30", employees[4]);
        assertEquals("1500", employees[5]);
        assertEquals("Tom", employees[6]);
        assertEquals("36", employees[7]);
        assertEquals("2000", employees[8]);
    }


    @Test
    @SuppressWarnings("unchecked")
    public void testLifecycle() throws IOException {
        testCompile(DETAIL_TEMPLATE_FILE_NAME);
        testCompile(MASTER_TEMPLATE_FILE_NAME);

        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        MultivaluedMap queryParamMap = new MultivaluedHashMap();
        queryParamMap.put("title", List.of("abba"));
        Mockito.when(uriInfo.getQueryParameters()).thenReturn(queryParamMap);

        testGenerate("pdf", uriInfo);
        testGenerate("xml", uriInfo);
        testGenerate("csv", uriInfo);
        testGenerate("xls", uriInfo);
        testGenerate("xlsx", uriInfo);
        testGenerate("docx", uriInfo);
        testGenerate("odt", uriInfo);
        testGenerate("ods", uriInfo);
    }

    private void testCompile(String templateFileName) throws IOException {
        try (InputStream inputStream = ReportServiceRestImplTest.class.getResourceAsStream(PATH + templateFileName + ".jrxml")) {
            Attachment attachment = new Attachment(templateFileName, inputStream,
                    new ContentDisposition("form-data; name=\"file\"; filename=\"" + templateFileName + ".jrxml" + "\""));
            Response response = reportService.compileReportTemplate(attachment);
            assertEquals(response.getStatus(), 200);
        }
    }

    private void testGenerate(String format, UriInfo uriInfo) {
        try (Response response = reportService.generateReport(MASTER_TEMPLATE_FILE_NAME, format, uriInfo)) {
            assertEquals(response.getStatus(), 200);
            assertTrue(response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION).get(0).toString().contains(MASTER_TEMPLATE_FILE_NAME + "." + format));
        }
    }

    private void initData(DataSource dataSource) throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.createStatement().execute(getSqlStatement());
        connection.close();
    }

    private String getSqlStatement() {
        return new BufferedReader(
                new InputStreamReader(ReportServiceRestImplTest.class.getResourceAsStream("/script/createEmployees.sql")))
                .lines().collect(Collectors.joining("\n"));
    }

    @After
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void removeFileStorageRoot() throws IOException {
        Files.walk(Paths.get(fileStorageRoot))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}