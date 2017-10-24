package com.hortonworks.streamline.storage;

import com.hortonworks.streamline.storage.impl.jdbc.transaction.TransactionManager;

public interface TransactionManagerAware {
    void setTransactionManager(TransactionManager transactionManager);
}
