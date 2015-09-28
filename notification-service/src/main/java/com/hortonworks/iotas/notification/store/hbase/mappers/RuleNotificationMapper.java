package com.hortonworks.iotas.notification.store.hbase.mappers;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.store.hbase.mappers.NotificationIndexMapper;

import java.util.Arrays;
import java.util.List;

/**
 * Create a [rule -> notification] mapping by inserting the
 * record into Rule_Notification table in HBase.
 */
public class RuleNotificationMapper extends NotificationIndexMapper {
    private static final String TABLE_NAME = "Rule_Notification";

    @Override
    protected List<byte[]> getRowKeys(Notification notification) {
        return Arrays.asList(new StringBuilder(notification.getRuleId())
                        .append(ROWKEY_SEP)
                        .append(System.currentTimeMillis())
                        .toString().getBytes(CHARSET));
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }
}
