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

package com.hortonworks.iotas.streams.notification.store.hbase.mappers;

import com.google.common.collect.Lists;
import com.hortonworks.iotas.common.util.ReflectionHelper;
import com.hortonworks.iotas.streams.notification.Notification;
import com.hortonworks.iotas.streams.notification.util.NotificationTestObjectFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hortonworks.iotas.streams.notification.store.hbase.mappers.AbstractNotificationMapper.CHARSET;
import static com.hortonworks.iotas.streams.notification.store.hbase.mappers.Mapper.ROWKEY_SEP;
import static org.junit.Assert.*;

public class NotificationIndexMappersTest {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationIndexMappersTest.class);

    private List<NotificationIndexMapper> testObjects = Lists.newArrayList(
            new DatasourceNotificationMapper(),
            new DatasourceStatusNotificationMapper(),
            new NotifierNotificationMapper(),
            new NotifierStatusNotificationMapper(),
            new RuleNotificationMapper(),
            new RuleStatusNotificationMapper());

    @Test
    public void testGetRowKeys() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Map<String, Object> fieldAndValues = new HashMap<>();
        fieldAndValues.put("one", "A");

        // assume test notification has one datasource and one event id
        // for simplicity
        Notification notification = NotificationTestObjectFactory.getOne();
        assertEquals(1, notification.getDataSourceIds().size());
        assertEquals(1, notification.getEventIds().size());

        for (NotificationIndexMapper testObject : testObjects) {
            LOG.info("testing mapper: {}", testObject.getClass().getCanonicalName());

            StringBuilder expectedRowKey = new StringBuilder();

            for (String indexedField : testObject.getIndexedFieldNames()) {
                Object value = null;

                try {
                    value = getValue(notification, indexedField);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    try {
                        value = getValue(notification, indexedField + "s");
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e2) {
                        fail("getter not found for indexed field " + indexedField + "(s)");
                    }
                }

                assertNotNull(value);
                expectedRowKey.append(value);
                expectedRowKey.append(ROWKEY_SEP);
            }

            expectedRowKey.append(generateExpectedIndexSuffix(notification, testObject));

            byte[] expectedRowKeyBytes = expectedRowKey.toString().getBytes(CHARSET);
            assertArrayEquals("getRowKeys() doesn't contain expected rowkey", expectedRowKeyBytes,
                    testObject.getRowKeys(notification).get(0));
        }

        // for exception case: TimestampNotificationMapper
        TimestampNotificationMapper testObject = new TimestampNotificationMapper();
        LOG.info("testing mapper: {}", testObject.getClass().getCanonicalName());
        assertArrayEquals(
                "getRowKeys() doesn't contain expected rowkey",
                generateExpectedIndexSuffix(notification, testObject).getBytes(CHARSET),
                testObject.getRowKeys(notification).get(0));
    }

    @Test
    public void testTableMutations() {
        // assume test notification has one datasource and one event id
        // for simplicity
        Notification notification = NotificationTestObjectFactory.getOne();
        assertEquals(1, notification.getDataSourceIds().size());
        assertEquals(1, notification.getEventIds().size());

        for (NotificationIndexMapper testObject : testObjects) {
            LOG.info("testing mapper: {}", testObject.getClass().getCanonicalName());

            List<byte[]> rowKeys = testObject.getRowKeys(notification);
            List<TableMutation> tableMutations = testObject.tableMutations(notification);
            assertEquals(rowKeys.size(), tableMutations.size());
            assertEquals(1, rowKeys.size());

            byte[] rowKey = rowKeys.get(0);
            TableMutation tableMutation = tableMutations.get(0);
            assertEquals(1, tableMutation.updates().size());
            assertTrue(tableMutation.deletes().isEmpty());

            assertArrayEquals(rowKey, tableMutation.updates().get(0).getRow());
        }
    }

    @Test
    public void testUpdateStatus() {
        // assume test notification has one datasource and one event id
        // for simplicity
        Notification notification = NotificationTestObjectFactory.getOne();
        assertEquals(1, notification.getDataSourceIds().size());
        assertEquals(1, notification.getEventIds().size());

        for (NotificationIndexMapper testObject : testObjects) {
            LOG.info("testing mapper: {}", testObject.getClass().getCanonicalName());

            List<byte[]> rowKeys = testObject.getRowKeys(notification);
            assertEquals(1, rowKeys.size());

            List<TableMutation> tableMutations = testObject.status(notification, Notification.Status.FAILED);

            Notification appliedStatus = NotificationTestObjectFactory.applyStatus(notification, Notification.Status.FAILED);

            List<byte[]> newRowKeys = testObject.getRowKeys(appliedStatus);
            assertEquals(1, newRowKeys.size());

            if (testObject instanceof NotificationStatusIndexMapper) {
                assertEquals(2, tableMutations.size());

                for (TableMutation tableMutation : tableMutations) {
                    if (!tableMutation.updates().isEmpty()) {
                        assertEquals(1, tableMutation.updates().size());
                        assertTrue(tableMutation.deletes().isEmpty());

                        // should add row with rowkey which reflects new status
                        assertArrayEquals(newRowKeys.get(0), tableMutation.updates().get(0).getRow());
                    } else if (!tableMutation.deletes().isEmpty()) {
                        assertTrue(tableMutation.updates().isEmpty());
                        assertEquals(1, tableMutation.deletes().size());

                        // should delete row with rowkey which reflects old status
                        assertArrayEquals(rowKeys.get(0), tableMutation.deletes().get(0).getRow());
                    } else {
                        fail("should have any updates or deletes");
                    }
                }
            } else {
                assertEquals(1, tableMutations.size());

                TableMutation tableMutation = tableMutations.get(0);
                assertEquals(1, tableMutation.updates().size());
                assertArrayEquals(newRowKeys.get(0), tableMutation.updates().get(0).getRow());

                assertTrue(tableMutation.deletes().isEmpty());
            }
        }
    }

    private String generateExpectedIndexSuffix(Notification notification, NotificationIndexMapper testObject) {
        // common rule for NotificationMapper
        return new StringBuilder().append(notification.getTs())
                .append(ROWKEY_SEP)
                .append(testObject.getUniqueIndexSuffix(notification)).toString();
    }

    private Object getValue(Notification notification, String indexedField) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object value = ReflectionHelper.invokeGetter(indexedField, notification);
        if (value instanceof List) {
            assertEquals(1, ((List) value).size());
            value = ((List)value).get(0);
        }
        return value;
    }

}
