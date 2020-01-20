package net.n2oapp.report.service.rest;

import net.n2oapp.report.api.ReportService;
import net.n2oapp.report.exception.ReportException;
import net.n2oapp.report.service.filestorage.FileStorage;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;

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
        InputStream reportInputStream = generate(template, format, parameters);

        return Response
                .ok(reportInputStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .header(CONTENT_DISPOSITION, buildContentDisposition(template, format))
                .build();
    }

    @Override
    public Response compileReportTemplate(Attachment attachment) {
        try {
            InputStream is = attachment.getDataHandler().getInputStream();

            if (isNull(attachment.getContentDisposition()) || StringUtils.isBlank(attachment.getContentDisposition().getFilename())) {
                throw new ReportException("Invalid file name");
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JasperCompileManager.compileReportToStream(is, outputStream);

            String fileName = getFileNameWithoutExtension(attachment.getContentDisposition().getFilename());
            fileStorage.saveContentWithFullPath(new ByteArrayInputStream(outputStream.toByteArray()), fileName + withLeadingDot(JASPER_EXTENSION));
        } catch (Exception e) {
            logger.error("File compile failed", e);
            throw new ReportException("File compile failed");
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

    private InputStream generate(String template, String format, Map<String, Object> params) {
        InputStream templateFileIO = fileStorage.getContent(template + withLeadingDot(JASPER_EXTENSION));
        try {
            JasperPrint jasperPrint = JasperFillManager.fillReport(templateFileIO, params);

            // todo use config files
            switch (format.toLowerCase()) {
                case "pdf":
                    return new ByteArrayInputStream(JasperExportManager.exportReportToPdf(jasperPrint));
                case "xml":
                    return new ByteArrayInputStream(JasperExportManager.exportReportToXml(jasperPrint).getBytes());
                case "csv" : {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    JRCsvExporter exporter = new JRCsvExporter();
                    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                    exporter.setExporterOutput(new SimpleWriterExporterOutput(outputStream));
                    SimpleCsvExporterConfiguration configuration = new SimpleCsvExporterConfiguration();
                    configuration.setWriteBOM(Boolean.TRUE);
                    configuration.setRecordDelimiter("\r\n");
                    exporter.setConfiguration(configuration);
                    exporter.exportReport();
                    return new ByteArrayInputStream(outputStream.toByteArray());
                }
                case "xlsx" : {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
                    configuration.setOnePagePerSheet(true);
                    configuration.setIgnoreGraphics(false);
                    JRXlsxExporter exporter = new JRXlsxExporter();
                    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                    exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
                    exporter.setConfiguration(configuration);
                    exporter.exportReport();
                    return new ByteArrayInputStream(outputStream.toByteArray());
                }
                case "xls" : {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
                    configuration.setOnePagePerSheet(true);
                    configuration.setIgnoreGraphics(false);
                    JRXlsExporter exporter = new JRXlsExporter();
                    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                    exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
                    exporter.setConfiguration(configuration);
                    exporter.exportReport();
                    return new ByteArrayInputStream(outputStream.toByteArray());
                }
                default: throw new ReportException("Invalid format");
            }

        } catch (JRException e) {
            // todo
            logger.error("failed");
            throw new ReportException("", e);
        }
    }
}