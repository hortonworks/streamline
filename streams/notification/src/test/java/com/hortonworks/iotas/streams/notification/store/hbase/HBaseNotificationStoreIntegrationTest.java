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

package com.hortonworks.iotas.streams.notification.store.hbase;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hortonworks.iotas.common.test.HBaseIntegrationTest;
import com.hortonworks.iotas.streams.notification.Notification;
import com.hortonworks.iotas.streams.notification.store.CriteriaImpl;
import com.hortonworks.iotas.streams.notification.store.hbase.mappers.DatasourceNotificationMapper;
import com.hortonworks.iotas.streams.notification.store.hbase.mappers.DatasourceStatusNotificationMapper;
import com.hortonworks.iotas.streams.notification.store.hbase.mappers.NotificationMapper;
import com.hortonworks.iotas.streams.notification.store.hbase.mappers.NotifierStatusNotificationMapper;
import com.hortonworks.iotas.streams.notification.store.hbase.mappers.RuleNotificationMapper;
import com.hortonworks.iotas.streams.notification.store.hbase.mappers.RuleStatusNotificationMapper;
import com.hortonworks.iotas.streams.notification.store.hbase.mappers.TableMutation;
import com.hortonworks.iotas.streams.notification.store.hbase.mappers.TimestampNotificationMapper;
import com.hortonworks.iotas.streams.notification.util.NotificationTestObjectFactory;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category(HBaseIntegrationTest.class)
public class HBaseNotificationStoreIntegrationTest {

    public static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static Connection connection;

    private static Map<String, List<String>> tableToCfs;

    static List<String> newList(String ...strings) {
        return Lists.newArrayList(strings);
    }

    static {
        tableToCfs = Maps.newHashMap();
        tableToCfs.put("Notification", newList("d", "f", "s", "e", "r", "nn", "ts"));
        tableToCfs.put("Timestamp_Notification", newList("ni", "d", "f", "s", "e", "r", "nn", "ts"));
        tableToCfs.put("Rule_Notification", newList("ni", "d", "f", "s", "e", "r", "nn", "ts"));
        tableToCfs.put("Rule_Status_Notification", newList("ni", "d", "f", "s", "e", "r", "nn", "ts"));
        tableToCfs.put("Datasource_Notification", newList("ni", "d", "f", "s", "e", "r", "nn", "ts"));
        tableToCfs.put("Datasource_Status_Notification", newList("ni", "d", "f", "s", "e", "r", "nn", "ts"));
        tableToCfs.put("Notifier_Notification", newList("ni", "d", "f", "s", "e", "r", "nn", "ts"));
        tableToCfs.put("Notifier_Status_Notification", newList("ni", "d", "f", "s", "e", "r", "nn", "ts"));
    }

    private HBaseNotificationStore sut;
    private NotificationMapper notificationMapper;

    @BeforeClass
    public static void setUpClass() throws IOException {
        connection = ConnectionFactory.createConnection();
        Admin admin = connection.getAdmin();

        for (Map.Entry<String, List<String>> tableInfo : tableToCfs.entrySet()) {
            TableName tableName = TableName.valueOf(tableInfo.getKey());
            if (!admin.tableExists(tableName)) {
                // table not found, creating
                HTableDescriptor tableDescriptor = new HTableDescriptor(tableName);
                for (String columnFamily : tableInfo.getValue()) {
                    tableDescriptor.addFamily(new HColumnDescriptor(columnFamily));
                }
                admin.createTable(tableDescriptor);
            }
        }
    }

    @Before
    public void setUp() throws IOException {
        try (Admin admin = connection.getAdmin()) {
            for (String tableNameStr : tableToCfs.keySet()) {
                TableName tableName = TableName.valueOf(tableNameStr);
                assertTrue(admin.tableExists(tableName));

                // full scan and delete: faster than 'disable and truncate' since we insert a little
                try (Table table = connection.getTable(tableName)) {
                    for (Result result : table.getScanner(new Scan())) {
                        table.delete(new Delete(result.getRow()));
                    }
                }
            }
        }

        // testing with default Hbase config
        sut = new HBaseNotificationStore();

        // it should be same as NotificationMapper in sut
        notificationMapper = new NotificationMapper(Lists.newArrayList(
                new NotifierStatusNotificationMapper(),
                new RuleNotificationMapper(),
                new RuleStatusNotificationMapper(),
                new DatasourceNotificationMapper(),
                new DatasourceStatusNotificationMapper(),
                new TimestampNotificationMapper()));
    }

    @Test
    public void testStoreNotification() throws IOException {
        Notification notification = NotificationTestObjectFactory.getOne();

        sut.store(notification);

        // rely on result of tableMutations() to avoid checking manually
        // didn't check each cell's value but comparing will be covered by testGetNotification()
        List<TableMutation> expectedMutations = notificationMapper.tableMutations(notification);
        checkMutationsReflectToHBase(expectedMutations);
    }

    @Test
    public void testGetNotification() throws IOException {
        Notification notification = NotificationTestObjectFactory.getOne();

        // covered by testStoreNotification()
        sut.store(notification);

        Notification fetchedNotification = sut.getNotification(notification.getId());
        assertEquals(notification, fetchedNotification);
    }

    @Test
    public void testGetNotifications() throws Exception {
        List<Notification> notifications = NotificationTestObjectFactory.getMany(10);

        for (Notification notification : notifications) {
            sut.store(notification);
        }

        List<Notification> fetchedNotifications = sut.getNotifications(
                Lists.transform(notifications, new Function<Notification, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Notification notification) {
                return notification.getId();
            }
        }));

        assertEquals(notifications, fetchedNotifications);
    }

    @Test
    public void testFindEntities() throws Exception {
        List<Notification> notifications = NotificationTestObjectFactory.getManyWithRandomTimestamp(10);

        for (Notification notification : notifications) {
            sut.store(notification);
        }

        Collections.sort(notifications, new Comparator<Notification>() {
            @Override
            public int compare(Notification o1, Notification o2) {
                return Long.valueOf(o1.getTs() - o2.getTs()).intValue();
            }
        });

        CriteriaImpl<Notification> criteria = new CriteriaImpl<>(Notification.class);
        criteria.setStartTs(notifications.get(2).getTs());
        criteria.setEndTs(notifications.get(6).getTs());
        // take less than range
        criteria.setNumRows(3);
        criteria.setDescending(true);

        // 5, 4, 3
        List<Notification> expectedList = notifications.subList(3, 6);
        Collections.sort(expectedList, new Comparator<Notification>() {
            @Override
            public int compare(Notification o1, Notification o2) {
                return Long.valueOf(o2.getTs() - o1.getTs()).intValue();
            }
        });

        List<Notification> fetchedNotifications = sut.findEntities(criteria);
        assertEquals(expectedList, fetchedNotifications);

    }

    @Test
    public void testUpdateNotificationStatus() throws Exception {
        Notification notification = NotificationTestObjectFactory.getOne();

        sut.store(notification);

        Notification.Status newStatus = Notification.Status.FAILED;
        Notification fetchedNotification = sut.updateNotificationStatus(notification.getId(), newStatus);

        assertEquals(fetchedNotification.getId(), notification.getId());
        assertEquals(fetchedNotification.getStatus(), newStatus);

        // rely on result of status() to avoid checking manually
        List<TableMutation> expectedMutations = notificationMapper.status(notification, newStatus);
        checkMutationsReflectToHBase(expectedMutations);
    }

    private void checkMutationsReflectToHBase(List<TableMutation> expectedMutations) throws IOException {
        for (TableMutation tableMutation : expectedMutations) {
            String tableName = tableMutation.tableName();
            try (Table hTable = connection.getTable(TableName.valueOf(tableName))) {
                for (Put put : tableMutation.updates()) {
                    // checks only rowkey
                    assertTrue(hTable.exists(new Get(put.getRow())));
                }

                for (Delete delete : tableMutation.deletes()) {
                    // checks only rowkey
                    assertFalse(hTable.exists(new Get(delete.getRow())));
                }
            }
        }
    }
}
