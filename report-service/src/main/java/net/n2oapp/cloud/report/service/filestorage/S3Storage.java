package net.n2oapp.cloud.report.service.filestorage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import lombok.RequiredArgsConstructor;
import net.n2oapp.cloud.report.exception.ReportException;
import org.springframework.beans.factory.annotation.Value;
import ru.i_novus.common.file.storage.FsFileStorage;

import java.io.InputStream;

import static org.springframework.util.StringUtils.hasText;

@RequiredArgsConstructor
public class S3Storage implements FsFileStorage {

    private final AmazonS3 s3client;

    @Value("${report-service.s3.bucket-name:reports}")
    private String bucketName;

    @Override
    public InputStream getContent(String path) {
        try {
            S3Object s3object = s3client.getObject(bucketName, path);
            return s3object.getObjectContent();
        } catch (Exception e) {
            throw new ReportException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public String saveContent(InputStream content, String name) {
        return saveContentWithFullPath(content, name);
    }

    @Override
    public String saveContentWithFullPath(InputStream content, String path) {
        createBucketIfNotExist();
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setCacheControl("max-age=2592000, must-revalidate");
            metadata.setContentLength(content.available());
            s3client.putObject(bucketName, path, content, metadata);
            return path;
        } catch (Exception e) {
            throw new ReportException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void removeContent(String path) {
        if (!hasText(path)) {
            return;
        }
        try {
            s3client.deleteObject(bucketName, path);
        } catch (Exception e) {
            throw new ReportException(e.getLocalizedMessage(), e);
        }
    }

    private void createBucketIfNotExist() {
        if (!s3client.doesBucketExistV2(bucketName))
            s3client.createBucket(bucketName);
    }

}
