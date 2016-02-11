package com.hortonworks.iotas.notification.store.hbase.mappers;

import org.apache.hadoop.hbase.client.Result;

import java.util.List;

/**
 * Maps the different entities like Notification,
 * IotasEvent, Rule etc into one or more HBase tables.
 */
public interface Mapper<T> {

    /**
     * The row key separator.
     */
    String ROWKEY_SEP = "|";

    /**
     * Returns a list of HBase table mutations for the entity type T
     */
    List<TableMutation> tableMutations(T t);

    /**
     * Maps the HBase row result back to entity.
     */
    T entity(Result result);

    /**
     * The mapping table name
     */
    String getTableName();

    /**
     * <p>
     * Return Column_Family, Column_Qualifier, Column_Value triplet for
     * an entity member name and value.
     * E.g. ["s".getBytes(), "qs".getBytes(), "NEW".getBytes()] for
     * {@code NotificationMapper.mapMemberValue("status", NEW)}.
     * If a memberName is not found, returns null.
     * </p>
     */
    List<byte[]> mapMemberValue(String memberName, String value);
}
