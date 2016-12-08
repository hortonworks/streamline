package org.apache.streamline.registries.model.service;

import java.io.InputStream;
import org.apache.streamline.common.util.FileStorage;
import org.apache.streamline.registries.model.data.ModelInfo;
import org.apache.streamline.storage.StorableKey;
import org.apache.streamline.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by schendamaraikannan on 12/5/16.
 */
public final class ModelRegistryService {
    private static final Logger LOG = LoggerFactory.getLogger(ModelRegistryService.class);
    private static final String MODEL_NAME_SPACE = new ModelInfo().getNameSpace();
    private final StorageManager storageManager;
    private final FileStorage fileStorage;

    public ModelRegistryService(StorageManager storageManager, FileStorage fileStorage) {
        this.storageManager = storageManager;
        this.fileStorage = fileStorage;
    }

    public ModelInfo addModelInfo(ModelInfo modelInfo) {
        if (modelInfo.getId() == null) {
            modelInfo.setId(storageManager.nextId(MODEL_NAME_SPACE));
        }

        LOG.debug("Adding model " + modelInfo.getModelName());
        modelInfo.setTimestamp(System.currentTimeMillis());
        this.storageManager.add(modelInfo);
        return modelInfo;
    }

    public void uploadFile(InputStream fileContents, String fileName) {
        try {
            fileStorage.uploadFile(fileContents, fileName);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public ModelInfo getModelInfo(Long modelId) {
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setId(modelId);
        return this.storageManager.get(new StorableKey(MODEL_NAME_SPACE, modelInfo.getPrimaryKey()));
    }

    public InputStream getModelFile(Long modelId) throws IOException {
        ModelInfo modelInfo = getModelInfo(modelId);
        return fileStorage.downloadFile(modelInfo.getPmmlFileName());
    }
}
