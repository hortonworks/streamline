package com.hortonworks.iotas.registries.tag.service;

import com.hortonworks.iotas.common.ModuleRegistration;
import com.hortonworks.iotas.common.util.FileStorage;
import com.hortonworks.iotas.storage.StorageManager;
import com.hortonworks.iotas.storage.StorageManagerAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation for the tag-registry module for registration with web service module
 */
public class TagRegistryModule implements ModuleRegistration, StorageManagerAware {
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
        TagService tagService = new CatalogTagService(storageManager);
        TagCatalogResource tagCatalogResource = new TagCatalogResource(tagService);
        result.add(tagCatalogResource);
        return result;
    }

    @Override
    public void setStorageManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }
}
