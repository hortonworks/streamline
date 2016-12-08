package org.apache.streamline.registries.model.service;

import org.apache.streamline.common.ModuleRegistration;
import org.apache.streamline.common.util.FileStorage;
import org.apache.streamline.storage.StorageManager;
import org.apache.streamline.storage.StorageManagerAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by schendamaraikannan on 12/5/16.
 */
public final class ModelRegistryModule implements ModuleRegistration, StorageManagerAware {
    private FileStorage fileStorage;
    private Map<String, Object> config;
    private StorageManager storageManager;

    @Override
    public void init(Map<String, Object> config, FileStorage fileStorage) {
        this.config = config;
        this.fileStorage = fileStorage;
    }

    @Override
    public List<Object> getResources() {
        List<Object> result = new ArrayList<>();
        ModelRegistryService modelService = new ModelRegistryService(storageManager, fileStorage);
        ModelRegistryResource modelCatalogResource = new ModelRegistryResource(modelService);
        result.add(modelCatalogResource);
        return result;
    }

    @Override
    public void setStorageManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }
}
