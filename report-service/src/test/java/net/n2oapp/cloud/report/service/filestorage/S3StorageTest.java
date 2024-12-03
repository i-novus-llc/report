package net.n2oapp.cloud.report.service.filestorage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class S3StorageTest {

    private S3Storage s3Storage;

    private AmazonS3 s3client;

    @BeforeEach
    public void setUp() {
        s3client = mock(AmazonS3.class);
        s3Storage = new S3Storage(s3client);
    }

    @Test
    void testGetContent() throws Exception {
        String contentStr = "test content";
        String path = "template.jasper";
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new ByteArrayInputStream(contentStr.getBytes()));

        when(s3client.getObject(any(), eq(path))).thenReturn(s3Object);

        InputStream content = s3Storage.getContent(path);
        assertNotNull(content);
        byte[] bytes = content.readAllBytes();
        String res = new String(bytes);
        assertEquals(contentStr,res);

        verify(s3client, times(1)).getObject(any(), eq(path));
    }

    @Test
    void testSaveContentWithFullPath() throws Exception {
        String path = "template.jasper";
        InputStream content = new ByteArrayInputStream("test content".getBytes());

        s3Storage.saveContentWithFullPath(content, path);
        verify(s3client, times(1)).putObject(any(), eq(path), eq(content), any(ObjectMetadata.class));
    }

    @Test
    void testRemoveContent() throws Exception {
        String path = "template.jasper";
        s3Storage.removeContent(path);
        verify(s3client, times(1)).deleteObject(any(), eq(path));
    }

}