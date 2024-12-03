package net.n2oapp.cloud.report.service.filestorage;

import org.junit.jupiter.api.Test;
import ru.i_novus.common.file.storage.FsFileStorage;

import static org.junit.jupiter.api.Assertions.*;

class StorageConfigurationTest {

    @Test
    void testStorageWithoutEndpoint() {
        StorageConfiguration conf = new StorageConfiguration();
        FsFileStorage storage = conf.storage(null, null, null);
        assertInstanceOf(FileStorage.class, storage);
    }

    @Test
    void testStorageWitEndpoint() {
        StorageConfiguration conf = new StorageConfiguration();
        FsFileStorage storage = conf.storage("admin", "admin", "http://test-endpoint");
        assertInstanceOf(S3Storage.class, storage);
    }

}