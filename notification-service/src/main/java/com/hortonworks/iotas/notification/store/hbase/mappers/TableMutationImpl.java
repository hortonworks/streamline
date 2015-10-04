package com.hortonworks.iotas.notification.store.hbase.mappers;

import org.apache.hadoop.hbase.client.Put;

import java.util.Arrays;
import java.util.List;

/**
 * The {@link TableMutation} implementation that holds the
 * table name and the list of rows to be inserted or updated.
 */
public class TableMutationImpl implements TableMutation {

    private final String tableName;
    private final List<Put> rows;

    public TableMutationImpl(String tableName, List<Put> rows) {
        this.tableName = tableName;
        this.rows = rows;
    }

    public TableMutationImpl(String tableName, Put row) {
        this.tableName = tableName;
        this.rows = Arrays.asList(row);
    }

    @Override
    public String tableName() {
        return tableName;
    }

    @Override
    public List<Put> rows() {
        return rows;
    }

    @Override
    public String toString() {
        return "TableMutationImpl{" +
                "tableName='" + tableName + '\'' +
                ", rows=" + rows +
                '}';
    }
}
