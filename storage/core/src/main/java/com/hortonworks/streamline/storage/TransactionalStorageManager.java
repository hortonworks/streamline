package com.hortonworks.streamline.storage;

import com.hortonworks.streamline.storage.exception.StorageException;

public interface TransactionalStorageManager extends StorageManager {

    /**
     * Begins the transaction
     */
    void beginTransaction() throws StorageException;


    /**
     * Discards the changes made to the storage layer and reverts to the last committed point
     */
    void rollbackTransaction() throws StorageException;


    /**
     * Flushes the changes made to the storage layer
     */
    void commitTransaction() throws StorageException;
}
