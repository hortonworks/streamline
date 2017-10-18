package com.hortonworks.streamline.storage.impl.jdbc.transaction;

import com.hortonworks.streamline.storage.CacheBackedStorageManager;
import com.hortonworks.streamline.storage.StorageManager;
import com.hortonworks.streamline.storage.TransactionalStorageManager;

public class TransactionManager {
    private final StorageManager storageManager;
    private final boolean isStorageManagerTransactional;

    public TransactionManager(StorageManager storageManager) {
        StorageManager tempStorageManager;
        if (storageManager instanceof CacheBackedStorageManager) {
            tempStorageManager = ((CacheBackedStorageManager) storageManager).getStorageManager();
        } else {
            tempStorageManager = storageManager;
        }
        this.storageManager = tempStorageManager;
        if (tempStorageManager instanceof TransactionalStorageManager)
            this.isStorageManagerTransactional = true;
        else
            this.isStorageManagerTransactional = false;
    }

    public void beginTransaction() {
        if (isStorageManagerTransactional)
            ((TransactionalStorageManager) storageManager).beginTransaction();
    }

    public void rollbackTransaction() {
        if (isStorageManagerTransactional)
            ((TransactionalStorageManager) storageManager).rollbackTransaction();
    }

    public void commitTransaction() {
        if (isStorageManagerTransactional)
            ((TransactionalStorageManager) storageManager).commitTransaction();
    }
}
