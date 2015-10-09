package com.hortonworks.iotas.notification.store.hbase.mappers;

import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The {@link TableMutation} implementation that holds the
 * table name and the rows to be inserted, updated or deleted.
 */
public class TableMutationImpl implements TableMutation {
    private final String tableName;
    private final List<Put> updates;
    private final List<Delete> deletes;

    public TableMutationImpl(String tableName, List<Put> updateRows, List<Delete> deletes) {
        this.tableName = tableName;
        this.updates = updateRows;
        this.deletes = deletes;
    }

    public TableMutationImpl(String tableName, List<Put> updates) {
        this(tableName, updates, new ArrayList<Delete>());
    }

    // The Arrays.asList() is wrapped in new ArrayList<>() since the list should be mutable (support remove).
    public TableMutationImpl(String tableName, Put update) {
        this(tableName, new ArrayList<>(Arrays.asList(update)), new ArrayList<Delete>());
    }

    public TableMutationImpl(String tableName, Delete delete) {
        this(tableName, new ArrayList<Put>(), new ArrayList<>(Arrays.asList(delete)));
    }


    public TableMutationImpl(String tableName, Put update, Delete delete) {
        this(tableName, new ArrayList<>(Arrays.asList(update)), new ArrayList<>(Arrays.asList(delete)));
    }

    @Override
    public String tableName() {
        return tableName;
    }

    @Override
    public List<Put> updates() {
        return updates;
    }

    @Override
    public List<Delete> deletes() {
        return deletes;
    }

    @Override
    public String toString() {
        return "TableMutationImpl{" +
                "tableName='" + tableName + '\'' +
                ", updates=" + updates +
                ", deletes=" + deletes +
                '}';
    }
}
