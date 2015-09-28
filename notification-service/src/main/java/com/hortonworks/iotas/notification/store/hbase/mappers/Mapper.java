package com.hortonworks.iotas.notification.store.hbase.mappers;

import com.hortonworks.iotas.notification.common.Notification;
import org.apache.hadoop.hbase.client.Result;

import java.util.List;

/**
 * This is for mapping the different entities like Notification,
 * IotasEvent, Rule etc into one or more HBase tables.
 *
 */
public interface Mapper<T> {
    /**
     * Returns a list of HBase table mutations for the entity type T
     */
    List<TableMutation> tableMutations(T t);

    /**
     * Maps the HBase row result back to entity.
     */
    T entity(Result result);
}
