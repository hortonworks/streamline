package com.hortonworks.streamline.storage.impl.jdbc.transaction;

import com.hortonworks.streamline.storage.exception.TransactionException;

import java.sql.Connection;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionBookKeeper {

    private static final TransactionBookKeeper transactionBookKeeper = new TransactionBookKeeper();

    protected final ConcurrentHashMap<Long, TransactionContext> threadIdToConnectionMap = new ConcurrentHashMap<>();

    private TransactionBookKeeper() {

    }

    public boolean hasActiveTransaction(Long threadId) {
        return threadIdToConnectionMap.containsKey(threadId);
    }

    public Connection getConnection(Long threadId) {
        return threadIdToConnectionMap.get(threadId).getConnection();
    }

    public void addTransaction(Long threadId, Connection connection) {
        if (!threadIdToConnectionMap.containsKey(threadId)) {
            threadIdToConnectionMap.put(threadId, new TransactionContext(connection));
        } else {
            throw new TransactionException(String.format("A transaction is already associated with thread id : %s", Long.toString(threadId)));
        }
    }

    public void incrementTransactionUse(Long threadId) {
        TransactionContext transactionContext = threadIdToConnectionMap.get(threadId);
        transactionContext.incrementNestedTransactionCount();
        threadIdToConnectionMap.put(threadId, transactionContext);
    }

    public boolean removeTransaction(Long threadId) {
        if (threadIdToConnectionMap.containsKey(threadId)) {
            TransactionContext transactionContext = threadIdToConnectionMap.get(threadId);
            transactionContext.decrementNestedTransactionCount();
            if (transactionContext.getNestedTransactionCount() == 0) {
                threadIdToConnectionMap.remove(threadId);
                return true;
            } else
                threadIdToConnectionMap.put(threadId, transactionContext);
            return false;
        } else
            throw new TransactionException(String.format("No transaction is associated with thread id : %s", Long.toString(threadId)));
    }

    public static TransactionBookKeeper get() {
        return transactionBookKeeper;
    }
}
