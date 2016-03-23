package com.hortonworks.iotas.notification.store.hbase.mappers;

import com.hortonworks.iotas.notification.Notification;

import java.util.Arrays;
import java.util.List;

/**
 * A mapper that indexes notifications based on ts.
 * i.e. ts:notification_id_suffix -> Notification
 */
public class TimestampNotificationMapper extends NotificationIndexMapper {
    /**
     * The HBase index table
     */
    private static final String TABLE_NAME = "Timestamp_Notification";
    /**
     * The notification field that is indexed
     */
    private static final List<String> INDEX_FIELD_NAMES = Arrays.asList("ts");

    @Override
    protected List<byte[]> getRowKeys(Notification notification) {
        return Arrays.asList(new StringBuilder()
                                     .append(notification.getTs())
                                     .append(ROWKEY_SEP)
                                     .append(getIndexSuffix(notification))
                                     .toString().getBytes(CHARSET));

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
