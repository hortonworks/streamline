package com.hortonworks.iotas.notification.notifiers.store.hbase;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.notification.common.Notification;

import com.hortonworks.iotas.notification.notifiers.store.hbase.mappers.DatasourceDeviceNotificationMapper;
import com.hortonworks.iotas.notification.notifiers.store.hbase.mappers.DatasourceStatusDeviceNotificationMapper;
import com.hortonworks.iotas.notification.notifiers.store.hbase.mappers.DeviceNotificationMapper;
import com.hortonworks.iotas.notification.notifiers.store.hbase.mappers.NotifierDeviceNotificationMapper;
import com.hortonworks.iotas.notification.notifiers.store.hbase.mappers.NotifierStatusDeviceNotificationMapper;
import com.hortonworks.iotas.notification.notifiers.store.hbase.mappers.RuleDeviceNotificationMapper;
import com.hortonworks.iotas.notification.notifiers.store.hbase.mappers.RuleStatusDeviceNotificationMapper;
import com.hortonworks.iotas.notification.notifiers.store.hbase.mappers.TimestampDeviceNotificationMapper;
import com.hortonworks.iotas.notification.store.Criteria;
import com.hortonworks.iotas.notification.store.NotificationStore;
import com.hortonworks.iotas.notification.store.NotificationStoreException;
import com.hortonworks.iotas.notification.store.hbase.HBaseScanConfig;
import com.hortonworks.iotas.notification.store.hbase.HBaseScanConfigBuilder;
import com.hortonworks.iotas.notification.store.hbase.mappers.IotasEventMapper;
import com.hortonworks.iotas.notification.store.hbase.mappers.NotificationIndexMapper;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class will be used to store the device notification, HBase is the underlying storage.
 */
public class HBaseDeviceNotificationStore implements NotificationStore {
    private static final Logger LOG = LoggerFactory.getLogger(HBaseDeviceNotificationStore.class);
    private Configuration configuration;
    private HConnection connection;
    /**
     * A map of table name to the HBase HTable instances.
     * Since the HTable instances are not thread safe, its wrapped
     * in {@link ThreadLocal}
     */
    private final Map<String, ThreadLocal<HTableInterface>> tables = new HashMap<>();

    /**
     * The mapper for converting notifications
     */
    private final DeviceNotificationMapper deviceNotificationMapper;

    /**
     * The mapper for converting iotas events
     */
    private final IotasEventMapper iotaseventMapper;

    /**
     * The index mappers for Notification secondary indexes
     */
    private List<NotificationIndexMapper> notificationIndexMappers = new ArrayList<>();

    private HBaseScanConfigBuilder hBaseScanConfigBuilder;

    public HBaseDeviceNotificationStore() {
        this(null);
    }

    public HBaseDeviceNotificationStore(Map<String, String> hbaseConfig) {
        try {
            LOG.info("Initializing HBaseDeviceNotificationStore");
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
            notificationIndexMappers.add(new NotifierDeviceNotificationMapper());
            notificationIndexMappers.add(new NotifierStatusDeviceNotificationMapper());
            notificationIndexMappers.add(new RuleDeviceNotificationMapper());
            notificationIndexMappers.add(new RuleStatusDeviceNotificationMapper());
            notificationIndexMappers.add(new DatasourceDeviceNotificationMapper());
            notificationIndexMappers.add(new DatasourceStatusDeviceNotificationMapper());
            notificationIndexMappers.add(new TimestampDeviceNotificationMapper());

            for (NotificationIndexMapper indexMapper : notificationIndexMappers) {
                tables.put(indexMapper.getTableName(), tlHTable(indexMapper.getTableName()));
            }

            deviceNotificationMapper = new DeviceNotificationMapper(notificationIndexMappers);
            tables.put(deviceNotificationMapper.getTableName(), tlHTable(deviceNotificationMapper.getTableName()));

            iotaseventMapper = new IotasEventMapper();
            tables.put(iotaseventMapper.getTableName(), tlHTable(iotaseventMapper.getTableName()));

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
            store(deviceNotificationMapper.tableMutations(notification));
        } catch (IOException ex) {
            throw new NotificationStoreException("Error storing notification, id: " + notification.getId(), ex);
        }
    }

    private void store(List<TableMutation> tableMutations) throws IOException {
        for (TableMutation tm : tableMutations) {
            LOG.debug("Insert/Update {} row(s), Delete {} row(s) in table {}",
                    tm.updates().size(), tm.deletes().size(), tm.tableName());
            HTableInterface table = tables.get(tm.tableName()).get();
            if (!tm.updates().isEmpty()) {
                table.put(tm.updates());
            }
            if (!tm.deletes().isEmpty()) {
                table.delete(tm.deletes());
            }
        }
    }

    @Override
    public Notification getNotification(String notificationId) {
        try {
            String tableName = deviceNotificationMapper.getTableName();
            LOG.debug("getting device notification with notificationId {} from table {}", notificationId, tableName);
            Get get = new Get(notificationId.getBytes(StandardCharsets.UTF_8));
            Result result = tables.get(tableName).get().get(get);
            return result.isEmpty() ? null : deviceNotificationMapper.entity(result);
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
            Result result = tables.get(tableName).get().get(get);
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
                if(criteria.isDescending()) {
                    scan = new Scan(stopRow, startRow);
                    scan.setReversed(true);
                } else {
                    scan = new Scan(startRow, stopRow);
                }
                scan.setFilter(scanConfig.filterList());
                ResultScanner scanner = tables.get(scanConfig.getMapper().getTableName()).get().getScanner(scan);
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
            for (ThreadLocal<HTableInterface> table : tables.values()) {
                LOG.debug("Closing table {}", table);
                table.get().close();
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
            store(deviceNotificationMapper.status(getNotification(notificationId), status));
            return getNotification(notificationId);
        } catch (IOException ex) {
            throw new NotificationStoreException("Error updating status, notification-id: " + notificationId, ex);
        }
    }

    /**
     * Return a {@link ThreadLocal} wrapped HTable
     */
    private ThreadLocal<HTableInterface> tlHTable(final String tableName) {
        return new ThreadLocal<HTableInterface>() {
            @Override protected HTableInterface initialValue() {
                try {
                    return connection.getTable(TableName.valueOf(tableName));
                } catch (IOException ex) {
                    throw new NotificationStoreException("error getting HTable", ex);
                }
            }
        };
    }
}