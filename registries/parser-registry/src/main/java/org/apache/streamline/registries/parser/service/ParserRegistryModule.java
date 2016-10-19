package org.apache.streamline.registries.parser.service;

import org.apache.streamline.common.ModuleRegistration;
import org.apache.streamline.common.util.FileStorage;
import org.apache.streamline.storage.StorageManager;
import org.apache.streamline.storage.StorageManagerAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation for the parser-registry module for registration with web service module
 */
public class ParserRegistryModule implements ModuleRegistration, StorageManagerAware {
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
        ParsersCatalogService parsersCatalogService = new ParsersCatalogService(storageManager, fileStorage);
        final ParserInfoCatalogResource parserResource = new ParserInfoCatalogResource(parsersCatalogService);
        result.add(parserResource);
        return result;
    }

    @Override
    public void setStorageManager(StorageManager storageManager) {
        this.storageManager = storageManager;

    }
}
