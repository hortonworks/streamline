package com.hortonworks.iotas.notification.store.hbase.mappers;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.store.hbase.mappers.NotificationIndexMapper;

import java.util.Arrays;
import java.util.List;

/**
 * Create a [notifier name -> notification] mapping by inserting the
 * record into Notifier_Notification table in HBase.
 */
public class NotifierNotificationMapper extends NotificationIndexMapper {
    private static final String TABLE_NAME = "Notifier_Notification";

    @Override
    protected List<byte[]> getRowKeys(Notification notification) {
        return Arrays.asList(new StringBuilder(notification.getNotifierName())
                                     .append(ROWKEY_SEP)
                                     .append(System.currentTimeMillis())
                                     .toString().getBytes(CHARSET));
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }
}
