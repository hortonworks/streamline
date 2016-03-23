package com.hortonworks.iotas.notification.store.hbase.mappers;

import com.hortonworks.iotas.notification.Notification;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A mapper for mapping to and from Notifications to HBase tables.
 * This also adds rows into the configured index tables
 * (e.g, Notifiers_Notification, Rule_Notification etc)
 */
public class NotificationMapper extends AbstractNotificationMapper {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationMapper.class);

    private final List<NotificationIndexMapper> indexMappers;
    private static final String TABLE_NAME = "Notification";

    public NotificationMapper() {
        this(new ArrayList<NotificationIndexMapper>());
    }

    public NotificationMapper(List<NotificationIndexMapper> indexMappers) {
        this.indexMappers = indexMappers;
    }

    @Override
    public List<TableMutation> tableMutations(Notification notification) {
        List<TableMutation> tableMutations = super.tableMutations(notification);
        for (NotificationIndexMapper im : indexMappers) {
            tableMutations.addAll(im.tableMutations(notification));
        }
        LOG.trace("Notification {}, tableMutations {}", notification, tableMutations);
        return tableMutations;
    }

    /**
     * Returns a list of table mutations for updating the notification status. This
     * takes care of updating the index tables, if the index table stores status.
     */
    public List<TableMutation> status(Notification notification, Notification.Status status) {
        List<TableMutation> tableMutations = super.status(notification, status);
        for (NotificationIndexMapper im : indexMappers) {
            tableMutations.addAll(im.status(notification, status));
        }
        LOG.trace("Notification {}, Status {}, tableMutations {}", notification, status, tableMutations);
        return tableMutations;
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
        return TABLE_NAME;
    }
}
