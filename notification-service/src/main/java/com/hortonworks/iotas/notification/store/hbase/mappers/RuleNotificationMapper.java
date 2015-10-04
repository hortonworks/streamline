package com.hortonworks.iotas.notification.store.hbase.mappers;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.store.hbase.mappers.NotificationIndexMapper;

import java.util.Arrays;
import java.util.List;

/**
 * Secondary index mapping for ruleId. This is to enable Notification
 * lookup based on ruleId.
 */
public class RuleNotificationMapper extends NotificationIndexMapper {
    /**
     * The HBase index table
     */
    private static final String TABLE_NAME = "Rule_Notification";
    /**
     * The notification field that is indexed
     */
    private static final String INDEX_FIELD_NAME = "ruleId";


    @Override
    protected List<byte[]> getRowKeys(Notification notification) {
        return Arrays.asList(new StringBuilder(notification.getRuleId())
                        .append(ROWKEY_SEP)
                        .append(notification.getTs())
                        .toString().getBytes(CHARSET));
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public String getIndexedFieldName() {
        return INDEX_FIELD_NAME;
    }
}
