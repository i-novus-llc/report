package net.n2oapp.cloud.report.service.rest;

import net.n2oapp.cloud.report.api.ReportService;
import net.n2oapp.cloud.report.exception.ReportException;
import net.n2oapp.cloud.report.service.exception.ReportGenerateException;
import net.n2oapp.cloud.report.service.filestorage.FileStorage;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXmlExporter;
import net.sf.jasperreports.engine.export.oasis.JROdsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Controller
public class ReportServiceRestImpl implements ReportService {

    private static final String CONTENT_DISP_WITHOUT_FILENAME = "attachment; filename=";
    private static final String JASPER_EXTENSION = "jasper";
    private static final String FILE_STORAGE_ROOT_PROPERTY_NAME = "fsRoot";
    private static final String PARAMETERS_PROPERTIES = "parameters.properties";
    private static final String REPORT_DATASOURCE_NAME = "REPORT_DATASOURCE_NAME";

    @Value("${fileStorage.root}")
    private String fileStorageRoot;

    @Autowired
    private FileStorage fileStorage;

    @Autowired
    private Map<String, DataSource> dataSourcesWithName;

    @Override
    public Response generateReport(String template, String format, UriInfo ui) {
        Map<String, Object> parameters = prepareParameters(ui.getQueryParameters());
        try (InputStream is = generate(template, format, parameters)) {

            return Response
                    .ok(is, MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .header(CONTENT_DISPOSITION, buildContentDisposition(template, format))
                    .build();
        } catch (Exception e) {
            throw new ReportException("Failed to generate report. " + e.getMessage(), e);
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
        } catch (IOException e) {
            throw new ReportException("Failed to save compiled report template (.jasper)" + e.getMessage(), e);
        } catch (Exception e) {
            throw new ReportException("Failed to compile report template. " + e.getMessage(), e);
        }
        return Response.ok().build();
    }

    private Map<String, Object> prepareParameters(MultivaluedMap<String, String> multivaluedMap) {
        Map<String, Object> resultMap = new HashMap<>();
        // parameters from .properties file
        addParametersFromProperties(resultMap);
        // query parameters
        addQueryParameters(resultMap, multivaluedMap);
        // file storage root parameter
        resultMap.put(FILE_STORAGE_ROOT_PROPERTY_NAME, fileStorageRoot);

        return resultMap;
    }

    private void addParametersFromProperties(Map<String, Object> resultMap) {
        Properties properties = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(PARAMETERS_PROPERTIES)) {
            if (nonNull(is)) {
                properties.load(is);
                properties.stringPropertyNames().forEach(propName -> resultMap.put(propName, properties.getProperty(propName)));
            }
        } catch (IOException e) {
            throw new ReportException("Failed to get properties from parameters.properties", e);
        }
    }

    private void addQueryParameters(Map<String, Object> resultMap, MultivaluedMap<String, String> multivaluedMap) {
        multivaluedMap.forEach((name, values) -> {
            if (!CollectionUtils.isEmpty(values))
                resultMap.put(name, (values.size() != 1) ? values : values.get(0));
        });

    }

    private String getFileNameWithoutExtension(String fileName) {
        int indexOfExt = fileName.lastIndexOf('.');
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

    @SuppressWarnings("unchecked")
    private InputStream generate(String template, String format, Map<String, Object> params) throws JRException, IOException, SQLException {
        InputStream templateFileIO = fileStorage.getContent(template + withLeadingDot(JASPER_EXTENSION));
        JasperReport report = (JasperReport) JRLoader.loadObject(templateFileIO);
        params = getCastedParams(params, report);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            DataSource dataSource = getDataSource(report, params);
            JasperPrint jasperPrint;
            if (dataSource != null) {
                jasperPrint = JasperFillManager.fillReport(report, params, dataSource.getConnection());
            } else {
                jasperPrint = JasperFillManager.fillReport(report, params);
            }


            Exporter exporter;
            ExporterOutput output;

            switch (format.toLowerCase()) {
                case "pdf": {
                    exporter = new JRPdfExporter();
                    output = new SimpleOutputStreamExporterOutput(os);
                    break;
                }
                case "xml": {
                    exporter = new JRXmlExporter();
                    output = new SimpleXmlExporterOutput(os);
                    break;
                }
                case "csv": {
                    exporter = new JRCsvExporter();
                    output = new SimpleWriterExporterOutput(os);
                    break;
                }
                case "xlsx": {
                    exporter = new JRXlsxExporter();
                    output = new SimpleOutputStreamExporterOutput(os);
                    break;
                }
                case "xls": {
                    exporter = new JRXlsExporter();
                    output = new SimpleOutputStreamExporterOutput(os);
                    break;
                }
                case "docx": {
                    exporter = new JRDocxExporter();
                    output = new SimpleOutputStreamExporterOutput(os);
                    break;
                }
                case "odt": {
                    exporter = new JROdtExporter();
                    output = new SimpleOutputStreamExporterOutput(os);
                    break;
                }
                case "ods": {
                    exporter = new JROdsExporter();
                    output = new SimpleOutputStreamExporterOutput(os);
                    break;
                }
                default:
                    throw new ReportException("Format not supported: " + format);
            }
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(output);
            exporter.exportReport();
            return new ByteArrayInputStream(os.toByteArray());
        }
    }

    private Map<String, Object> getCastedParams(Map<String, Object> params, JasperReport report) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Optional<JRParameter> jrParameter = Stream.of(report.getParameters()).filter(param -> param.getName().equals(entry.getKey())).findAny();
            if (jrParameter.isPresent()) {
                result.put(entry.getKey(), castParam(entry.getValue(), jrParameter.get()));
            } else {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private Object castParam(Object value, JRParameter jrParameter) {
        try {
            return jrParameter.getValueClass()
                    .getDeclaredConstructor(value.getClass()).newInstance(value);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new ReportGenerateException(e);
        }
    }

    private String getReportDatasourceName(JasperReport report, Map<String, Object> params) {
        if (params.containsKey(REPORT_DATASOURCE_NAME)) {
            return params.get(REPORT_DATASOURCE_NAME).toString();
        }

        for (JRParameter jrParameter : report.getParameters()) {
            if (jrParameter.getName().equals(REPORT_DATASOURCE_NAME)) {
                return jrParameter.getDefaultValueExpression().getText();
            }
        }

        return null;
    }

    private DataSource getDataSource(JasperReport report, Map<String, Object> params) {
        String dataSourceName = getReportDatasourceName(report, params);
        if (dataSourceName == null) {
            return null;
        }
        return dataSourcesWithName.get(dataSourceName);
    }

}