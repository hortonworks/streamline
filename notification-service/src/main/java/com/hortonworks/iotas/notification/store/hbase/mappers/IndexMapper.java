package com.hortonworks.iotas.notification.store.hbase.mappers;

/**
 * Index mappers are for enabling secondary index based look ups based on
 * index tables. (e.g. Notifier_Notification)
 */
public interface IndexMapper<T> extends Mapper<T> {

    /**
     * The secondary index field name. E.g ('notifierName' in case of
     * NotifierNotificationMapper that enables look up based on notifier name)
     */
    String getIndexedFieldName();
}
