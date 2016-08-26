package com.hortonworks.iotas.storage;

/**
 * An interface for independent modules to implement so that the storage manager used by iotas can be injected
 */
public interface StorageManagerAware {
    void setStorageManager (StorageManager storageManager);
}
