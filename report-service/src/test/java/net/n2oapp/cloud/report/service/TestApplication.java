package net.n2oapp.cloud.report.service;

import com.amazonaws.services.s3.AmazonS3;
import net.n2oapp.cloud.report.service.filestorage.FileStorage;
import net.n2oapp.cloud.report.service.filestorage.S3Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;
import ru.i_novus.common.file.storage.FsFileStorage;

import static org.mockito.Mockito.mock;

@SpringBootApplication
public class TestApplication {

    @MockBean
    AmazonS3 amazonS3;

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    @Bean
    @Primary
    FsFileStorage storage(@Value("${report-service.s3.access-key:#{null}}") String accessKey,
                          @Value("${report-service.s3.secret-key:#{null}}") String secretKey,
                          @Value("${report-service.s3.endpoint:#{null}}") String endpoint) {
        return (StringUtils.hasText(endpoint))
                ? new S3Storage(amazonS3)
                : new FileStorage();
    }
}