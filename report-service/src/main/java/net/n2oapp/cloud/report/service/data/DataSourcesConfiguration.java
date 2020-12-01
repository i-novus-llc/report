package net.n2oapp.cloud.report.service.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class DataSourcesConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DataSourcesConfiguration.class);

    @Autowired
    private ReportDataSourceProperties reportDataSourceProperties;

    @Bean
    Map<String, DataSource> dataSources() {
        if (reportDataSourceProperties.getValues() == null) {
            logger.info("empty data sources");
            return Collections.emptyMap();
        }
        return reportDataSourceProperties.getValues().stream()
                .collect(Collectors.toMap(DataSourceValue::getName, this::createDataSource));
    }

    private DataSource createDataSource(DataSourceValue dataSourceValue) {
        if (dataSourceValue == null)
            return null;

        DriverManagerDataSource dataSource
                = new DriverManagerDataSource();
        dataSource.setDriverClassName(dataSourceValue.getDriver());
        dataSource.setUrl(dataSourceValue.getUrl());
        dataSource.setUsername(dataSourceValue.getUsername());
        dataSource.setPassword(dataSourceValue.getPassword());

        return dataSource;
    }
}
