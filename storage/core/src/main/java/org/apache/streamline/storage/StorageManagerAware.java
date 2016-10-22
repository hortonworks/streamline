package org.apache.streamline.storage;

/**
 * An interface for independent modules to implement so that the storage manager used by streamline can be injected
 */
public interface StorageManagerAware {
    void setStorageManager (StorageManager storageManager);
}
