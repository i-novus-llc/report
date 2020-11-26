package net.n2oapp.cloud.report.api;

import io.swagger.annotations.*;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/report")
@Api("Отчеты")
public interface ReportService {

    @GET
    @ApiOperation(value = "Сформировать отчет", notes = "Пример: /report/form345.pdf?param1=p1?param2=p2")
    @Path("/{template}.{format}")
    Response generateReport(
            @ApiParam("Наименование шаблона отчета (без расширения)") @PathParam("template") String template,
            @ApiParam("Формат отчета (pdf, xml, csv, xls, xlsx, docx, odt, ods)") @PathParam("format") String format,
            @ApiParam("Параметры отчета") @Context UriInfo uriInfo);

    @POST
    @ApiOperation("Скомпилировать и сохранить шаблон отчета в файловое хранилище (существующий затирается)")
    @ApiImplicitParams(@ApiImplicitParam(name = "file", value = "XML-шаблон отчета", required = true, dataType = "java.io.File", paramType = "form"))
    Response compileReportTemplate(
            @ApiParam(hidden = true, value = "Файл", required = true) @NotNull @Multipart(value = "file") Attachment attachment);
}