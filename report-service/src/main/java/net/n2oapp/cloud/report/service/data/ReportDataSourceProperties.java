package net.n2oapp.cloud.report.service.data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "report.data")
public class ReportDataSourceProperties {

    private List<DataSourceValue> values;

    public List<DataSourceValue> getValues() {
        return values;
    }

    public void setValues(List<DataSourceValue> values) {
        this.values = values;
    }
}
