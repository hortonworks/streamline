package com.hortonworks.iotas.notification.store.hbase;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.store.Criteria;
import com.hortonworks.iotas.notification.store.NotificationStore;
import com.hortonworks.iotas.notification.store.NotificationStoreException;
import com.hortonworks.iotas.notification.store.hbase.mappers.DatasourceNotificationMapper;
import com.hortonworks.iotas.notification.store.hbase.mappers.IotasEventMapper;
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
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.PageFilter;
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

    private Configuration configuration;
    private HConnection connection;
    /**
     * A map of table name to the HBase HTable instances.
     */
    private final Map<String, HTableInterface> tables = new HashMap<>();

    /**
     * The mapper for converting notifications
     */
    private final NotificationMapper notificationMapper;

    /**
     * The mapper for converting iotas events
     */
    private final IotasEventMapper iotaseventMapper;

    /**
     * The index mappers for Notification secondary indexes
     */
    private List<NotificationIndexMapper> notificationIndexMappers = new ArrayList<>();

    private HBaseScanConfigBuilder hBaseScanConfigBuilder;

    public HBaseNotificationStore() {
        this(null);
    }

    public HBaseNotificationStore(Map<String, String> hbaseConfig) {
        try {
            LOG.info("Initializing HBaseNotificationStore");
            configuration = HBaseConfiguration.create();
            /*
             * Override with the passed config.
             */
            if (hbaseConfig != null) {
                LOG.info("Overriding default HBase config with {}", hbaseConfig);
                for (Map.Entry<String, String> entry : hbaseConfig.entrySet()) {
                    configuration.set(entry.getKey(), entry.getValue());
                }
            }
            connection = HConnectionManager.createConnection(configuration);
            notificationIndexMappers.add(new NotifierNotificationMapper());
            notificationIndexMappers.add(new RuleNotificationMapper());
            notificationIndexMappers.add(new DatasourceNotificationMapper());
            for (NotificationIndexMapper indexMapper : notificationIndexMappers) {
                tables.put(indexMapper.getTableName(),
                           connection.getTable(TableName.valueOf(indexMapper.getTableName())));
            }
            notificationMapper = new NotificationMapper(notificationIndexMappers);
            tables.put(notificationMapper.getTableName(),
                       connection.getTable(TableName.valueOf(notificationMapper.getTableName())));

            iotaseventMapper = new IotasEventMapper();
            tables.put(iotaseventMapper.getTableName(),
                       connection.getTable(TableName.valueOf(iotaseventMapper.getTableName())));

            hBaseScanConfigBuilder = new HBaseScanConfigBuilder();
            hBaseScanConfigBuilder.addMappers(Notification.class, notificationIndexMappers);
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
            LOG.debug("Insert/Update {} row(s) in table {}", tm.rows().size(), tm.tableName());
            HTableInterface table = tables.get(tm.tableName());
            table.put(tm.rows());
        }
    }

    @Override
    public Notification getNotification(String notificationId) {
        try {
            String tableName = notificationMapper.getTableName();
            LOG.debug("getting notification with notificationId {} from table {}", notificationId, tableName);
            Get get = new Get(notificationId.getBytes(StandardCharsets.UTF_8));
            Result result = tables.get(tableName).get(get);
            return result.isEmpty() ? null : notificationMapper.entity(result);
        } catch (IOException ex) {
            throw new NotificationStoreException("Error getting notification id: " + notificationId, ex);
        }
    }

    @Override
    public List<Notification> getNotifications(List<String> notificationIds) {
        List<Notification> notifications = new ArrayList<>();
        for (String notificationId : notificationIds) {
            notifications.add(getNotification(notificationId));
        }
        return notifications;
    }

    @Override
    public IotasEvent getEvent(String eventId) {
        try {
            String tableName = iotaseventMapper.getTableName();
            LOG.debug("getting event with eventId {} from table {}", eventId, tableName);
            Get get = new Get(eventId.getBytes(StandardCharsets.UTF_8));
            Result result = tables.get(tableName).get(get);
            return result.isEmpty() ? null : iotaseventMapper.entity(result);
        } catch (IOException ex) {
            throw new NotificationStoreException("Error getting event id: " + eventId, ex);
        }
    }

    @Override
    public List<IotasEvent> getEvents(List<String> eventIds) {
        List<IotasEvent> events = new ArrayList<>();
        for (String eventId : eventIds) {
            events.add(getEvent(eventId));
        }
        return events;
    }

    @Override
    public <T> List<T> findEntities(Criteria<T> criteria) {
        List<T> entities = new ArrayList<>();
        LOG.debug("Finding entities from HBaseNotificationStore, Criteria {}", criteria);
        try {
            HBaseScanConfig<T> scanConfig = hBaseScanConfigBuilder.getScanConfig(criteria);
            LOG.debug("HBaseScanConfig for scan {}", scanConfig);
            if (scanConfig != null) {
                // From start to end row
                byte[] startRow = scanConfig.getStartRow();
                byte[] stopRow = scanConfig.getStopRow();
                Scan scan;
                if (startRow != null && stopRow != null) {
                    scan = new Scan(startRow, stopRow);
                } else {
                    scan = new Scan(); // all rows
                }
                scan.setFilter(scanConfig.filterList());
                ResultScanner scanner = tables.get(scanConfig.getMapper().getTableName()).getScanner(scan);
                for (Result result : scanner) {
                    entities.add(scanConfig.getMapper().entity(result));
                }
            }
        } catch (IOException ex) {
            throw new NotificationStoreException("Error during scan", ex);
        }

        return entities;
    }

    @Override
    public void close() {
        try {
            for (HTableInterface table : tables.values()) {
                LOG.debug("Closing table {}", table);
                table.close();
            }
            LOG.debug("Closing connection {}", connection);
            connection.close();
        } catch (IOException ex) {
            LOG.error("Got exception in close", ex);
        }
    }

    @Override
    public Notification updateNotificationStatus(String notificationId, Notification.Status status) {
        try {
            store(notificationMapper.status(getNotification(notificationId), status));
            return getNotification(notificationId);
        } catch (IOException ex) {
            throw new NotificationStoreException("Error updating status, notification-id: " + notificationId, ex);
        }
    }
}
