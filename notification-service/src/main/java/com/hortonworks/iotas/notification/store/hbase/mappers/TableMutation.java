package com.hortonworks.iotas.notification.store.hbase.mappers;

import org.apache.hadoop.hbase.client.Put;

import java.util.List;

/**
 * Represents a list of rows as HBase PUTs to be inserted/updated in a table.
 */
public interface TableMutation {

    /**
     * The table name.
     */
    String tableName();

    /**
     * the rows to be inserted or updated as Put requests.
     */
    List<Put> rows();
}
