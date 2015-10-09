package com.hortonworks.iotas.notification.store.hbase.mappers;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationImpl;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Secondary index mapping for notifier name + status to notification
 */
public class NotifierStatusNotificationMapper extends NotificationStatusIndexMapper {
    /**
     * The HBase index table
     */
    private static final String TABLE_NAME = "Notifier_Status_Notification";
    /**
     * The notification field that is indexed
     */
    private static final String INDEX_FIELD_NAME = "notifierName" + Mapper.ROWKEY_SEP + "status";


    @Override
    protected List<byte[]> getRowKeys(Notification notification) {
        return Arrays.asList(new StringBuilder(notification.getNotifierName())
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
