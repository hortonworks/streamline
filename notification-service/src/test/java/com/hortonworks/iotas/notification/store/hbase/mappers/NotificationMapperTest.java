package com.hortonworks.iotas.notification.store.hbase.mappers;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationImpl;
import com.hortonworks.iotas.notification.store.hbase.Serializer;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

/**
 * Created by aiyer on 10/6/15.
 */
@RunWith(JMockit.class)
public class NotificationMapperTest {

    NotificationMapper notificationMapper;

    @Mocked
    Result mockResult;

    @Before
    public void setUp() {
        notificationMapper = new NotificationMapper();
    }

    @Test
    public void testTableMutations() throws Exception {
        Map<String, Object> fieldAndValues = new HashMap<>();
        fieldAndValues.put("one", "A");
        Notification notification = new NotificationImpl.Builder(fieldAndValues)
                .id("notification-id")
                .dataSourceIds(Arrays.asList("dsrcid-1"))
                .eventIds(Arrays.asList("eventid-1"))
                .notifierName("test-notifier")
                .ruleId("rule-1")
                .build();

        List<TableMutation> tms = notificationMapper.tableMutations(notification);

        System.out.println(tms);

        assertEquals(1, tms.size());
        TableMutation tm = tms.get(0);

        assertEquals("Notification", tm.tableName());
        assertEquals(1, tm.updates().size());
        Put put = tm.updates().get(0);
        assertTrue(put.has("f".getBytes(), "one".getBytes(), new Serializer().serialize("A")));
        assertTrue(put.has("d".getBytes(), "dsrcid-1".getBytes(), "1".getBytes()));
        assertTrue(put.has("e".getBytes(), "eventid-1".getBytes(), "1".getBytes()));
        assertTrue(put.has("nn".getBytes(), "test-notifier".getBytes(), "1".getBytes()));
        assertTrue(put.has("r".getBytes(), "rule-1".getBytes(), "1".getBytes()));
        assertTrue(put.has("s".getBytes(), "qs".getBytes(), "NEW".getBytes()));
    }

    @Test
    public void testEntity() throws Exception {
        final Map<byte[], byte[]> tsMap = new TreeMap<>(new Bytes.ByteArrayComparator());
        tsMap.put("1444042473518".getBytes(), "1".getBytes());

        new Expectations() {
            {
                mockResult.getRow(); times=1;
                result = "nid-1".getBytes();
                mockResult.getValue("s".getBytes(), "qs".getBytes()); times = 1;
                result = "DELIVERED".getBytes();
                mockResult.getFamilyMap("ts".getBytes()); times = 1;
                result = tsMap;
            }
        };

        Notification notification = notificationMapper.entity(mockResult);
        //System.out.println(notification);
        assertEquals(notification.getId(), "nid-1");
        assertEquals(notification.getStatus(), Notification.Status.DELIVERED);
        assertEquals(notification.getTs(), 1444042473518L);
    }
}