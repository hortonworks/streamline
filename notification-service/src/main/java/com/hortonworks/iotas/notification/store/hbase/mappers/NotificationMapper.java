package com.hortonworks.iotas.notification.store.hbase.mappers;

import com.hortonworks.iotas.notification.common.Notification;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import java.util.Arrays;
import java.util.List;

/**
 * A mapper for mapping to and from Notifications to HBase tables.
 * This also adds rows into the configured index tables
 * (e.g, Notifiers_Notification, Rule_Notification etc)
 */
public class NotificationMapper extends AbstractNotificationMapper {
    public static final String TABLE_NOTIFICATION = "Notification";

    private final List<NotificationIndexMapper> indexMappers;

    public NotificationMapper(List<NotificationIndexMapper> indexMappers) {
        this.indexMappers = indexMappers;
    }

    @Override
    public List<TableMutation> tableMutations(Notification notification) {
        List<TableMutation> tableMutations = super.tableMutations(notification);
        for (NotificationIndexMapper im : indexMappers) {
            tableMutations.addAll(im.tableMutations(notification));
        }
        return tableMutations;
    }

    /**
     * Returns a put object for updating the notification status.
     */
    public static Put status(String notificationId, Notification.Status status) {
        Put put = new Put(notificationId.getBytes(CHARSET));
        put.add(CF_STATUS, CQ_STATUS, status.toString().getBytes(CHARSET));
        return put;
    }

    @Override
    protected List<byte[]> getRowKeys(Notification notification) {
        return Arrays.asList(notification.getId().getBytes(CHARSET));
    }

    @Override
    protected String getNotificationId(Result result) {
        return Bytes.toString(result.getRow());
    }

    @Override
    public String getTableName() {
        return TABLE_NOTIFICATION;
    }
}
