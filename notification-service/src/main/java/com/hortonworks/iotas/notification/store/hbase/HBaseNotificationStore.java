package com.hortonworks.iotas.notification.store.hbase;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.store.Criteria;
import com.hortonworks.iotas.notification.store.NotificationStore;
import com.hortonworks.iotas.notification.store.NotificationStoreException;
import com.hortonworks.iotas.notification.store.hbase.mappers.DatasourceNotificationMapper;
import com.hortonworks.iotas.notification.store.hbase.mappers.NotificationIndexMapper;
import com.hortonworks.iotas.notification.store.hbase.mappers.NotificationMapper;
import com.hortonworks.iotas.notification.store.hbase.mappers.NotifierNotificationMapper;
import com.hortonworks.iotas.notification.store.hbase.mappers.RuleNotificationMapper;
import com.hortonworks.iotas.notification.store.hbase.mappers.TableMutation;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Notification store implementation with HBase as the underlying storage.
 */
public class HBaseNotificationStore implements NotificationStore {

    private static final Logger LOG = LoggerFactory.getLogger(HBaseNotificationStore.class);

    private static final String TABLE_NAME = "Notification";


    private Configuration configuration;
    private HConnection connection;
    /**
     * Later we might add tables automatically based on indexes.
     */
    private final Map<String, HTableInterface> tables = new HashMap<>();

    // A mapper for converting notification
    private final NotificationMapper notificationMapper;

    private List<NotificationIndexMapper> indexMappers = new ArrayList<>();

    public HBaseNotificationStore() {
        try {
            LOG.info("Initializing HBaseNotificationStore");
            Configuration conf = new Configuration();
            configuration = HBaseConfiguration.create();
            connection = HConnectionManager.createConnection(configuration);
            tables.put(TABLE_NAME, connection.getTable(TableName.valueOf(TABLE_NAME)));
            indexMappers.add(new NotifierNotificationMapper());
            indexMappers.add(new RuleNotificationMapper());
            indexMappers.add(new DatasourceNotificationMapper());
            for (NotificationIndexMapper indexMapper: indexMappers) {
                tables.put(indexMapper.getTableName(),
                           connection.getTable(TableName.valueOf(indexMapper.getTableName())));
            }
            notificationMapper = new NotificationMapper(indexMappers);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void store(Notification notification) {
        try {
            LOG.debug("Storing notification {} in HBase", notification);
            store(notificationMapper.tableMutations(notification));
        } catch (IOException ex) {
            throw new NotificationStoreException("Error storing notification, id: " + notification.getId(), ex);
        }
    }

    private void store(List<TableMutation> tableMutations) throws IOException {
        for (TableMutation tm : tableMutations) {
            LOG.debug("Insert/Update {} rows in table {}", tm.rows().size(), tm.tableName());
            HTableInterface table = tables.get(tm.tableName());
            table.put(tm.rows());
        }
    }

    @Override
    public void setDelivered(String notificationId) {
        try {
            updateStatus(notificationId, Notification.Status.DELIVERED);
        } catch (IOException ex) {
            throw new NotificationStoreException("Error updating status, notification-id: " + notificationId, ex);
        }
    }

    @Override
    public void setFailed(String notificationId) {
        try {
            updateStatus(notificationId, Notification.Status.FAILED);
        } catch (IOException ex) {
            throw new NotificationStoreException("Error updating status, notification-id: " + notificationId, ex);
        }
    }

    private void updateStatus(String notificationId, Notification.Status status) throws IOException {
        tables.get(NotificationMapper.TABLE_NOTIFICATION)
                .put(NotificationMapper.status(notificationId, status));
        //TODO: update index tables
    }

    @Override
    public Notification get(String notificationId) {
        try {
            Get get = new Get(notificationId.getBytes(StandardCharsets.UTF_8));
            Result result = tables.get(NotificationMapper.TABLE_NOTIFICATION).get(get);
            return notificationMapper.entity(result);
        } catch (IOException ex) {
            throw new NotificationStoreException("Error getting notification id: " + notificationId, ex);
        }
    }

    @Override
    public List<Notification> find(Criteria criteria) {
        return null;
    }

    @Override
    public void close() {
        try {
            for (HTableInterface table : tables.values()) {
                table.close();
            }
            connection.close();
        } catch (IOException ex) {
            LOG.error("Got exception in close", ex);
        }
    }
}
