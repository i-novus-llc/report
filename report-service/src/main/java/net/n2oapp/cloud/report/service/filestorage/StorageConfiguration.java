package net.n2oapp.cloud.report.service.filestorage;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.util.AwsHostNameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import ru.i_novus.common.file.storage.FsFileStorage;

@Configuration
public class StorageConfiguration {

    @Bean
    FsFileStorage storage(@Value("${report-service.s3.access-key:#{null}}") String accessKey,
                          @Value("${report-service.s3.secret-key:#{null}}") String secretKey,
                          @Value("${report-service.s3.endpoint:#{null}}") String endpoint) {
        return (StringUtils.hasText(endpoint)) ? createS3Storage(accessKey, secretKey, endpoint)
                : new FileStorage();
    }

    private S3Storage createS3Storage(String accessKey, String secretKey, String endpoint) {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        AwsClientBuilder.EndpointConfiguration endpointConfiguration =
                new AwsClientBuilder.EndpointConfiguration(endpoint,
                        AwsHostNameUtils.parseRegion(endpoint, AmazonS3Client.S3_SERVICE_NAME));
        AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(endpointConfiguration)
                .build();
        return new S3Storage(amazonS3);
    }

}
