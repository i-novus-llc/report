package net.n2oapp.report.service.rest;

import net.n2oapp.report.api.ReportService;
import net.n2oapp.report.exception.ReportException;
import net.n2oapp.report.service.filestorage.FileStorage;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXmlExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXmlExporterOutput;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Controller
public class ReportServiceRestImpl implements ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportServiceRestImpl.class);

    private static final String CONTENT_DISP_WITHOUT_FILENAME = "attachment; filename=";
    private static final String JASPER_EXTENSION = "jasper";

    @Autowired
    private FileStorage fileStorage;

    @Override
    public Response generateReport(String template, String format, UriInfo ui) {
        Map<String, Object> parameters = prepareParameters(ui.getQueryParameters());
        try (InputStream is = generate(template, format, parameters)) {

            return Response
                    .ok(is, MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .header(CONTENT_DISPOSITION, buildContentDisposition(template, format))
                    .build();
        } catch (JRException|IOException e) {
            throw new ReportException("Failed to generate report", e);
        }
    }

    @Override
    public Response compileReportTemplate(Attachment attachment) {
        if (isNull(attachment.getContentDisposition()) || isBlank(attachment.getContentDisposition().getFilename()))
            throw new ReportException("Invalid file name");

        try (InputStream is = attachment.getDataHandler().getInputStream();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            JasperCompileManager.compileReportToStream(is, os);
            String fileName = getFileNameWithoutExtension(attachment.getContentDisposition().getFilename());

            try (InputStream iss = new ByteArrayInputStream(os.toByteArray())) {
                fileStorage.saveContentWithFullPath(iss, fileName + withLeadingDot(JASPER_EXTENSION));
            }
        } catch (JRException e) {
            throw new ReportException("Failed to compile report template");
        } catch (IOException e) {
            throw new ReportException("Failed to save compiled report template (.jasper)");
        }
        return Response.ok().build();
    }

    private static Map<String, Object> prepareParameters(MultivaluedMap<String, String> multivaluedMap) {
        Map<String, Object> result = new HashMap<>();
        multivaluedMap.forEach((name, values) -> {
            if (!CollectionUtils.isEmpty(values))
                result.put(name, (values.size() != 1) ? values : values.get(0));
        });
        return result;
    }

    private String getFileNameWithoutExtension(String fileName) {
        int indexOfExt = fileName.lastIndexOf(".");
        return indexOfExt < 0
                ? fileName
                : fileName.substring(0, indexOfExt);
    }

    private static String buildContentDisposition(String fileName, String extension) {
        return CONTENT_DISP_WITHOUT_FILENAME + "\"" + fileName + withLeadingDot(extension) + "\"";
    }

    private static String withLeadingDot(String str) {
        return str.charAt(0) != '.' ? "." + str : str;
    }

    private InputStream generate(String template, String format, Map<String, Object> params) throws JRException, IOException {
        InputStream templateFileIO = fileStorage.getContent(template + withLeadingDot(JASPER_EXTENSION));

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            JasperPrint jasperPrint = JasperFillManager.fillReport(templateFileIO, params);

            switch (format.toLowerCase()) {
                case "pdf": {
                    JRPdfExporter exporter = new JRPdfExporter();
                    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                    exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(os));
                    exporter.exportReport();
                    break;
                }
                case "xml": {
                    JRXmlExporter exporter = new JRXmlExporter();
                    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                    exporter.setExporterOutput(new SimpleXmlExporterOutput(os));
                    exporter.exportReport();
                    break;
                }
                case "csv": {
                    JRCsvExporter exporter = new JRCsvExporter();
                    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                    exporter.setExporterOutput(new SimpleWriterExporterOutput(os));
                    exporter.exportReport();
                    break;
                }
                case "xlsx": {
                    JRXlsxExporter exporter = new JRXlsxExporter();
                    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                    exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(os));
                    exporter.exportReport();
                    break;
                }
                case "xls": {
                    JRXlsExporter exporter = new JRXlsExporter();
                    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                    exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(os));
                    exporter.exportReport();
                    break;
                }
                case "docx": {
                    JRDocxExporter exporter = new JRDocxExporter();
                    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                    exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(os));
                    exporter.exportReport();
                    break;
                }
                default:
                    throw new ReportException("Invalid report format");
            }
            return new ByteArrayInputStream(os.toByteArray());
        }
    }
}