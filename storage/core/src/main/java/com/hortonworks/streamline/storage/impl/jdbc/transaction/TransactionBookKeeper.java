package com.hortonworks.streamline.storage.impl.jdbc.transaction;

import com.hortonworks.streamline.storage.exception.TransactionException;

import java.sql.Connection;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionBookKeeper {

    private static final TransactionBookKeeper transactionBookKeeper = new TransactionBookKeeper();

    protected final ConcurrentHashMap<Long, Connection> threadIdToConnectionMap = new ConcurrentHashMap<>();

    private TransactionBookKeeper() {

    }

    public boolean hasActiveTransaction(Long threadId) {
        return threadIdToConnectionMap.containsKey(threadId);
    }

    public Connection getConnection(Long threadId) {
        return threadIdToConnectionMap.get(threadId);
    }

    public void addTransaction(Long threadId, Connection connection) {
        if (!threadIdToConnectionMap.containsKey(threadId)) {
            threadIdToConnectionMap.put(threadId, connection);
        } else
            throw new TransactionException(String.format("A transaction is already associated with thread id : %s ", Long.toString(threadId)));
    }

    public void removeTransaction(Long threadId) {
        if (threadIdToConnectionMap.containsKey(threadId)) {
            threadIdToConnectionMap.remove(threadId);
        } else
            throw new TransactionException(String.format("No transaction is associated with thread id : %s", Long.toString(threadId)));
    }

    public static TransactionBookKeeper get() {
        return transactionBookKeeper;
    }
}
