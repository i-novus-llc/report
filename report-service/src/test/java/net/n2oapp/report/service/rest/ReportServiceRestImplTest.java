package net.n2oapp.report.service.rest;

import junit.framework.TestCase;
import net.n2oapp.platform.test.autoconfigure.DefinePort;
import net.n2oapp.report.api.ReportService;
import net.n2oapp.report.service.ReportApplication;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.ws.rs.core.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = ReportApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "fileStorage.root=./fs",
                "cxf.jaxrs.component-scan=true",
                "cxf.jaxrs.client.classes-scan-packages=net.n2oapp.report",
                "jaxrs.log-in=false",
                "jaxrs.log-out=false"
        })
@DefinePort
public class ReportServiceRestImplTest extends TestCase {

    private static final String PATH = "/net/n2oapp/report/service/rest/";
    // todo replace test templates use other
    private static final String MASTER_TEMPLATE_FILE_NAME = "accrCommand";
    private static final String DETAIL_TEMPLATE_FILE_NAME = "command";

    @Value("${fileStorage.root}")
    private String fileStorageRoot;

    @Autowired
    private ReportService reportService;

    @Test
    @SuppressWarnings("unchecked")
    public void testLifecycle() throws IOException {
        testCompile(MASTER_TEMPLATE_FILE_NAME);
        testCompile(DETAIL_TEMPLATE_FILE_NAME);

        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        MultivaluedMap queryParamMap = new MultivaluedHashMap();
        queryParamMap.put("status", List.of("ACCREDITED"));
        Mockito.when(uriInfo.getQueryParameters()).thenReturn(queryParamMap);

        testGenerate("pdf", uriInfo);
        testGenerate("xml", uriInfo);
        testGenerate("csv", uriInfo);
        testGenerate( "xls", uriInfo);
        testGenerate( "xlsx", uriInfo);
        testGenerate( "docx", uriInfo);
        testGenerate( "odt", uriInfo);
        testGenerate( "ods", uriInfo);
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

    @After
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void removeFileStorageRoot() throws IOException {
        Files.walk(Paths.get(fileStorageRoot))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}