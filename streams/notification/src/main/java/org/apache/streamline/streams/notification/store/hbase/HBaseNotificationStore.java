/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.streamline.streams.notification.store.hbase;

import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.notification.Notification;
import org.apache.streamline.streams.notification.store.Criteria;
import org.apache.streamline.streams.notification.store.NotificationStore;
import org.apache.streamline.streams.notification.store.NotificationStoreException;
import org.apache.streamline.streams.notification.store.hbase.mappers.DatasourceNotificationMapper;
import org.apache.streamline.streams.notification.store.hbase.mappers.DatasourceStatusNotificationMapper;
import org.apache.streamline.streams.notification.store.hbase.mappers.StreamlineEventMapper;
import org.apache.streamline.streams.notification.store.hbase.mappers.NotificationIndexMapper;
import org.apache.streamline.streams.notification.store.hbase.mappers.NotificationMapper;
import org.apache.streamline.streams.notification.store.hbase.mappers.NotifierNotificationMapper;
import org.apache.streamline.streams.notification.store.hbase.mappers.NotifierStatusNotificationMapper;
import org.apache.streamline.streams.notification.store.hbase.mappers.RuleNotificationMapper;
import org.apache.streamline.streams.notification.store.hbase.mappers.RuleStatusNotificationMapper;
import org.apache.streamline.streams.notification.store.hbase.mappers.TableMutation;
import org.apache.streamline.streams.notification.store.hbase.mappers.TimestampNotificationMapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
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
    private Connection connection;
    /**
     * A map of table name to the HBase HTable instances.
     * Since the HTable instances are not thread safe, its wrapped
     * in {@link ThreadLocal}
     */
    private final Map<String, ThreadLocal<Table>> tables = new HashMap<>();

    /**
     * The mapper for converting notifications
     */
    private final NotificationMapper notificationMapper;

    /**
     * The mapper for converting iotas events
     */
    private final StreamlineEventMapper eventMapper;

    /**
     * The index mappers for Notification secondary indexes
     */
    private final List<NotificationIndexMapper> notificationIndexMappers = new ArrayList<>();

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
            connection = ConnectionFactory.createConnection(configuration);
            notificationIndexMappers.add(new NotifierNotificationMapper());
            notificationIndexMappers.add(new NotifierStatusNotificationMapper());
            notificationIndexMappers.add(new RuleNotificationMapper());
            notificationIndexMappers.add(new RuleStatusNotificationMapper());
            notificationIndexMappers.add(new DatasourceNotificationMapper());
            notificationIndexMappers.add(new DatasourceStatusNotificationMapper());
            notificationIndexMappers.add(new TimestampNotificationMapper());
            for (NotificationIndexMapper indexMapper : notificationIndexMappers) {
                tables.put(indexMapper.getTableName(), tlHTable(indexMapper.getTableName()));
            }
            notificationMapper = new NotificationMapper(notificationIndexMappers);
            tables.put(notificationMapper.getTableName(), tlHTable(notificationMapper.getTableName()));

            eventMapper = new StreamlineEventMapper();
            tables.put(eventMapper.getTableName(), tlHTable(eventMapper.getTableName()));

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
            LOG.debug("Insert/Update {} row(s), Delete {} row(s) in table {}",
                      tm.updates().size(), tm.deletes().size(), tm.tableName());
            Table table = tables.get(tm.tableName()).get();
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
            String tableName = notificationMapper.getTableName();
            LOG.debug("getting notification with notificationId {} from table {}", notificationId, tableName);
            Get get = new Get(notificationId.getBytes(StandardCharsets.UTF_8));
            Result result = tables.get(tableName).get().get(get);
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
    public StreamlineEvent getEvent(String eventId) {
        try {
            String tableName = eventMapper.getTableName();
            LOG.debug("getting event with eventId {} from table {}", eventId, tableName);
            Get get = new Get(eventId.getBytes(StandardCharsets.UTF_8));
            Result result = tables.get(tableName).get().get(get);
            return result.isEmpty() ? null : eventMapper.entity(result);
        } catch (IOException ex) {
            throw new NotificationStoreException("Error getting event id: " + eventId, ex);
        }
    }

    @Override
    public List<StreamlineEvent> getEvents(List<String> eventIds) {
        List<StreamlineEvent> events = new ArrayList<>();
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
            for (ThreadLocal<Table> table : tables.values()) {
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
            store(notificationMapper.status(getNotification(notificationId), status));
            return getNotification(notificationId);
        } catch (IOException ex) {
            throw new NotificationStoreException("Error updating status, notification-id: " + notificationId, ex);
        }
    }

    /**
     * Return a {@link ThreadLocal} wrapped HTable
     */
    private ThreadLocal<Table> tlHTable(final String tableName) {
        return new ThreadLocal<Table>() {
            @Override protected Table initialValue() {
                try {
                    return connection.getTable(TableName.valueOf(tableName));
                } catch (IOException ex) {
                    throw new NotificationStoreException("error getting HTable", ex);
                }
            }
        };
    }
}
