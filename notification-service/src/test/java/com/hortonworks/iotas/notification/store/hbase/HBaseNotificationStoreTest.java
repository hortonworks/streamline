package com.hortonworks.iotas.notification.store.hbase;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationImpl;
import com.hortonworks.iotas.notification.store.Criteria;
import com.hortonworks.iotas.notification.store.CriteriaImpl;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

@RunWith(JMockit.class)
public class HBaseNotificationStoreTest {

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    HBaseNotificationStore notificationStore;

    Notification notification;

    @Mocked
    HConnection mockHConnection;

    @Mocked
    HTableInterface mockHTable;

    @Mocked
    Result mockResult;

    @Mocked
    Criteria<Notification> mockCriteria;

    @Mocked
    ResultScanner mockResultScanner;


    @Before
    public void setUp() {

        new MockUp<HBaseConfiguration>() {

        };

        new MockUp<HConnectionManager>() {
            @Mock
            void $clinit() {

            }

            @Mock
            HConnection createConnection(Configuration configuration) {
                return mockHConnection;
            }
        };

        Map<String, Object> fv = new HashMap<>();
        fv.put("temp", "100");
        notification = new NotificationImpl.Builder(fv)
                .id("id1")
                .eventIds(Arrays.asList("ev1"))
                .dataSourceIds(Arrays.asList("d1"))
                .ruleId("ruleId")
                .notifierName("notifierName")
                .timestamp(System.currentTimeMillis())
                .status(Notification.Status.NEW).build();

        notificationStore = new HBaseNotificationStore();

    }

    @Test
    public void testStore() throws Exception {
        notificationStore.store(notification);

        new Verifications() {
            {
                List<Put> puts;
                mockHTable.put(puts = withCapture()); times = 8;
                //System.out.println("puts = " + puts);
            }
        };
    }

    @Test
    public void testGetNotification() throws Exception {
        final Map<byte[], byte[]> tsMap = new TreeMap<>(new Bytes.ByteArrayComparator());
        tsMap.put("1444042473518".getBytes(), "1".getBytes());
        new Expectations() {
            {
                mockResult.getRow(); times = 1;
                result = "rowid".getBytes(CHARSET);
                mockResult.getValue("s".getBytes(), "qs".getBytes()); times = 1;
                result = "DELIVERED".getBytes();
                mockResult.getFamilyMap("ts".getBytes()); times = 1;
                result = tsMap;
            }
        };

        Notification notification = notificationStore.getNotification("n123");
        //System.out.println(notification);
        assertEquals("rowid", notification.getId());
        assertEquals(Notification.Status.DELIVERED, notification.getStatus());
        assertEquals(1444042473518L, notification.getTs());
        new Verifications() {
            {
                Get get;
                mockHTable.get(get = withCapture()); times = 1;
                //System.out.println("get = " + get);
            }
        };
    }


    @Test
    public void testFindEntities() throws Exception {
        final List<Criteria.Field> fr = new ArrayList<>();
        fr.add(new CriteriaImpl.FieldImpl("ruleId", "1"));
        fr.add(new CriteriaImpl.FieldImpl("status", "NEW"));

        final Map<byte[], byte[]> tsMap = new TreeMap<>(new Bytes.ByteArrayComparator());
        tsMap.put("1444042473518".getBytes(), "1".getBytes());

        final Map<byte[], byte[]> niMap = new TreeMap<>(new Bytes.ByteArrayComparator());
        niMap.put("nid".getBytes(), "1".getBytes());

        final List<Result> results = Arrays.asList(mockResult);

        new Expectations() {
            {
                mockCriteria.clazz(); times = 1;
                result = Notification.class;
                mockCriteria.fieldRestrictions(); times = 1;
                result = fr;
                mockHTable.getScanner(withAny(new Scan())); times = 1;
                result = mockResultScanner;
                mockResultScanner.iterator();
                result = results.iterator();
                mockResult.getFamilyMap("ni".getBytes()); times = 1;
                result = niMap;
                mockResult.getValue("s".getBytes(), "qs".getBytes()); times = 1;
                result = "NEW".getBytes();
                mockResult.getFamilyMap("ts".getBytes()); times = 1;
                result = tsMap;
            }
        };
        List<Notification> notifications = notificationStore.findEntities(mockCriteria);
        System.out.println(notifications);
        assertEquals("nid", notifications.get(0).getId());
        assertEquals(Notification.Status.NEW, notifications.get(0).getStatus());
        assertEquals(1444042473518L, notifications.get(0).getTs());

        new Verifications() {
            {
                Scan scan;
                mockHTable.getScanner(scan = withCapture()); times = 1;
                System.out.println("Scan = " + scan);
            }
        };
    }

}