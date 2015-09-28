package com.hortonworks.iotas.notification.store.hbase.mappers;

import org.apache.hadoop.hbase.client.Put;

import java.util.List;

/**
 * Represents a list of rows that can be inserted into a HBase table.
 */
public interface TableMutation {

    String tableName();

    List<Put> rows();
}
