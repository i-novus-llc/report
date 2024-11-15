package net.n2oapp.cloud.report.service.rest;

import net.n2oapp.cloud.report.api.ReportService;
import net.n2oapp.cloud.report.exception.ReportException;
import net.n2oapp.cloud.report.service.TestApplication;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.ws.rs.core.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = {"classpath:application-test.properties"})
class ReportServiceRestImplTest {

    private static final String PATH = "/net/n2oapp/report/service/rest/";
    private static final String MASTER_TEMPLATE_FILE_NAME = "masterReportTemplate";
    private static final String DETAIL_TEMPLATE_FILE_NAME = "detailReportTemplate";
    private static final String EXAMPLE_TEMPLATE_FILE_NAME = "exampleReportTemplate";
    private static final String PARAMS_TEMPLATE_FILE_NAME = "paramsReportTemplate";
    private static final String IN_MEMORY_DB_NAME = "in_memory_db";

    @Value("${fileStorage.root}")
    private String fileStorageRoot;

    @Autowired
    private ReportService reportService;

    @AfterEach
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void removeFileStorageRoot() throws IOException {
        Files.walk(Paths.get(fileStorageRoot))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    void testGetCsvReportFromInMemoryDb() throws Exception {
        testCompile(EXAMPLE_TEMPLATE_FILE_NAME);

        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        MultivaluedMap<String, String> queryParamMap = new MultivaluedHashMap<>();
        queryParamMap.put("REPORT_DATASOURCE_NAME", List.of(IN_MEMORY_DB_NAME));
        Mockito.when(uriInfo.getQueryParameters()).thenReturn(queryParamMap);

        try (Response response = reportService.generateReport(EXAMPLE_TEMPLATE_FILE_NAME, "csv", uriInfo)) {
            assertEquals(200, response.getStatus());
            assertTrue(response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION).get(0).toString().contains(EXAMPLE_TEMPLATE_FILE_NAME + "." + "csv"));
            compareContents(response);
        }

    }

    @Test
    void testLifecycle() throws IOException {
        testCompile(DETAIL_TEMPLATE_FILE_NAME);
        testCompile(MASTER_TEMPLATE_FILE_NAME);

        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        MultivaluedMap<String, String> queryParamMap = new MultivaluedHashMap<>();
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

    @Test
    void testCollectionParams() throws Exception {
        testCompile(PARAMS_TEMPLATE_FILE_NAME);

        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        MultivaluedMap<String, String> queryParamMap = new MultivaluedHashMap<>();
        queryParamMap.put("REPORT_DATASOURCE_NAME", List.of(IN_MEMORY_DB_NAME));
        queryParamMap.put("ages", List.of("25", "30"));
        Mockito.when(uriInfo.getQueryParameters()).thenReturn(queryParamMap);

        try (Response response = reportService.generateReport(PARAMS_TEMPLATE_FILE_NAME, "csv", uriInfo)) {
            assertEquals(200, response.getStatus());
            assertTrue(response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION).get(0).toString().contains(PARAMS_TEMPLATE_FILE_NAME + "." + "csv"));

            ByteArrayInputStream inputStream = (ByteArrayInputStream) response.getEntity();
            String content = new String(inputStream.readAllBytes());
            String[] employees = content.split("\n");
            String firstEmployee = employees[0];
            String firstEmployeeName = firstEmployee.substring(employees[0].indexOf("Misha"));
            assertEquals("Misha", firstEmployeeName);
            assertEquals("25", employees[1]);
            assertEquals("1000", employees[2]);
            assertEquals("Sasha", employees[3]);
            assertEquals("30", employees[4]);
            assertEquals("1500", employees[5]);
        }
    }

    @Test
    void testWrongTypeCollectionParams() throws Exception {
        testCompile(PARAMS_TEMPLATE_FILE_NAME);

        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        MultivaluedMap<String, String> queryParamMap = new MultivaluedHashMap<>();
        queryParamMap.put("REPORT_DATASOURCE_NAME", List.of(IN_MEMORY_DB_NAME));
        queryParamMap.put("ages", List.of("qwerty"));
        Mockito.when(uriInfo.getQueryParameters()).thenReturn(queryParamMap);

        assertThrows(ReportException.class, () -> reportService.generateReport(PARAMS_TEMPLATE_FILE_NAME, "csv", uriInfo));
    }

    private void compareContents(Response response) {
        ByteArrayInputStream inputStream = (ByteArrayInputStream) response.getEntity();
        String content = new String(inputStream.readAllBytes());
        String[] employees = content.split("\n");
        String firstEmployee = employees[0];
        String firstEmployeeName = firstEmployee.substring(employees[0].indexOf("Misha"));
        assertEquals("Misha", firstEmployeeName);
        assertEquals("25", employees[1]);
        assertEquals("1000", employees[2]);
        assertEquals("Sasha", employees[3]);
        assertEquals("30", employees[4]);
        assertEquals("1500", employees[5]);
        assertEquals("Kolya", employees[6]);
        assertEquals("36", employees[7]);
        assertEquals("2000", employees[8]);
    }

    private void testCompile(String templateFileName) throws IOException {
        try (InputStream inputStream = ReportServiceRestImplTest.class.getResourceAsStream(PATH + templateFileName + ".jrxml")) {
            Attachment attachment = new Attachment(templateFileName, inputStream,
                    new ContentDisposition("form-data; name=\"file\"; filename=\"" + templateFileName + ".jrxml" + "\""));
            Response response = reportService.compileReportTemplate(attachment);
            assertEquals(200, response.getStatus());
        }
    }

    private void testGenerate(String format, UriInfo uriInfo) {
        try (Response response = reportService.generateReport(MASTER_TEMPLATE_FILE_NAME, format, uriInfo)) {
            assertEquals(200, response.getStatus());
            assertTrue(response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION).get(0).toString().contains(MASTER_TEMPLATE_FILE_NAME + "." + format));
        }
    }

}