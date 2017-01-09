package org.apache.streamline.streams.notification.store.hbase.mappers;

import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;

import java.util.List;

/**
 * Holds a list of inserts/updates/deletes to a HBase table
 */
public interface TableMutation {

    /**
     * The table name.
     */
    String tableName();

    /**
     * the rows to be inserted or updated as Put requests.
     */
    List<Put> updates();


    /**
     * the rows to be deleted as Delete requests.
     */
    List<Delete> deletes();
}
