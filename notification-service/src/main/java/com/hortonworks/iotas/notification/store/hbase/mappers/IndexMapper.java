package com.hortonworks.iotas.notification.store.hbase.mappers;

import java.util.List;

/**
 * Index mappers are for enabling secondary index based look ups based on
 * index tables. (e.g. Notifier_Notification)
 */
public interface IndexMapper<T> extends Mapper<T> {

    /**
     * The secondary index field names. E.g (['notifierName', 'status'] in case of
     * NotifierStatusNotificationMapper that enables look up based on notifier name)
     */
    List<String> getIndexedFieldNames();
}
