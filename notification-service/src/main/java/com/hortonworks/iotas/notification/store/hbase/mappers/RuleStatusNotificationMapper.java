package com.hortonworks.iotas.notification.store.hbase.mappers;

import com.hortonworks.iotas.notification.common.Notification;

import java.util.Arrays;
import java.util.List;

/**
 * Secondary index mapping for ruleId + status to notification
 */
public class RuleStatusNotificationMapper extends NotificationStatusIndexMapper {
    /**
     * The HBase index table
     */
    private static final String TABLE_NAME = "Rule_Status_Notification";
    /**
     * The notification field that is indexed
     */
    private static final String INDEX_FIELD_NAME = "ruleId" + Mapper.ROWKEY_SEP + "status";

    @Override
    protected List<byte[]> getRowKeys(Notification notification) {
        return Arrays.asList(new StringBuilder(notification.getRuleId())
                                     .append(ROWKEY_SEP)
                                     .append(notification.getStatus())
                                     .append(ROWKEY_SEP)
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
    public String getIndexedFieldName() {
        return INDEX_FIELD_NAME;
    }
}
