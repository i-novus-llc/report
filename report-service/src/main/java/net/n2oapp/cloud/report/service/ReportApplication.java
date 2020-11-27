package net.n2oapp.cloud.report.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("net.n2oapp.cloud.report")
public class ReportApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(ReportApplication.class, args);
    }
}