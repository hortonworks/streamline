package com.hortonworks.iotas.registries.parser.service;

import com.hortonworks.iotas.common.ModuleRegistration;
import com.hortonworks.iotas.common.util.FileStorage;
import com.hortonworks.iotas.storage.StorageManager;
import com.hortonworks.iotas.storage.StorageManagerAware;

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
