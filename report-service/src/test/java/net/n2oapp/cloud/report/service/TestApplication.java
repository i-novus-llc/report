package net.n2oapp.cloud.report.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SyncTaskExecutor;


@SpringBootApplication(exclude = QuartzAutoConfiguration.class)
public class TestApplication {

    //Отключение Async
    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(new SyncTaskExecutor());
        return eventMulticaster;
    }

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

}