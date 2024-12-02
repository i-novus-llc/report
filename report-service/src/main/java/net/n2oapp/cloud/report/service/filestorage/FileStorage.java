package net.n2oapp.cloud.report.service.filestorage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.i_novus.common.file.storage.BaseFileStorage;

@Component
@ConditionalOnProperty(value = "report-service.s3.enabled", havingValue = "false")
public class FileStorage extends BaseFileStorage {

    @Override
    protected String getWorkspaceName() {
        return null;
    }

    @Override
    protected String getSpaceName() {
        return null;
    }
}