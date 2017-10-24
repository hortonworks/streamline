package com.hortonworks.streamline.storage.impl.jdbc.transaction;

import java.sql.Connection;

public class TransactionContext {
    private int nestedTransactionCount = 1;
    private final Connection connection;

    public TransactionContext(Connection connection) {
        this.connection = connection;
    }

    public void incrementNestedTransactionCount() {
        this.nestedTransactionCount++;
    }

    public void decrementNestedTransactionCount() {
        this.nestedTransactionCount--;
    }

    public Connection getConnection() {
        return this.connection;
    }

    public int getNestedTransactionCount() {
        return this.nestedTransactionCount;
    }
}
