package com.hortonworks.iotas.notification.notifiers.store.hbase.mappers;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.store.hbase.mappers.NotificationIndexMapper;

import java.util.Arrays;
import java.util.List;

/**
 * A mapper that indexes device notifications based on ts.
 * i.e. ts:notification_id_suffix -> Notification
 */
public class TimestampDeviceNotificationMapper extends NotificationIndexMapper {
    /**
     * The HBase index table
     */
    private static final String TABLE_NAME = "Timestamp_Device_Notification";

    /**
     * The device notification field that is indexed
     */
    private static final List<String> INDEX_FIELD_NAMES = Arrays.asList("ts");

    @Override
    protected List<byte[]> getRowKeys(Notification notification) {
        return Arrays.asList(getIndexSuffix(notification).getBytes(CHARSET));
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public List<String> getIndexedFieldNames() {
        return INDEX_FIELD_NAMES;
    }
}
