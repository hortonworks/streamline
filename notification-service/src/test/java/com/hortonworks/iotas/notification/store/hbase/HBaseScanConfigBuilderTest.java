package com.hortonworks.iotas.notification.store.hbase;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.store.Criteria;
import com.hortonworks.iotas.notification.store.CriteriaImpl;
import com.hortonworks.iotas.notification.store.hbase.mappers.IndexMapper;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(JMockit.class)
public class HBaseScanConfigBuilderTest {

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    HBaseScanConfigBuilder hBaseScanConfigBuilder;

    @Mocked
    IndexMapper<Notification> mockIndexMapper;

    @Mocked
    Criteria<Notification> mockNotificationCriteria;

    @Test
    public void testGetScanConfigInvalid() throws Exception {
        hBaseScanConfigBuilder = new HBaseScanConfigBuilder();
        hBaseScanConfigBuilder.addMappers(Notification.class, Arrays.asList(mockIndexMapper));

        Criteria<IotasEvent> iotasEventCriteria = new CriteriaImpl<>(IotasEvent.class);

        HBaseScanConfig<IotasEvent> iotasEventScanConfig = hBaseScanConfigBuilder.getScanConfig(iotasEventCriteria);
        assertNull(iotasEventScanConfig);
    }

    @Test
    public void testGetScanConfig() throws Exception {
        final Map<String, String> frMap = new HashMap<>();
        frMap.put("notifierName", "test_notifier");
        frMap.put("status", "NEW");

        final List<byte[]> nnList = Arrays.asList("s".getBytes(CHARSET),
                                                  "qs".getBytes(CHARSET),
                                                  "NEW".getBytes(CHARSET));
        new Expectations() {
            {
                mockNotificationCriteria.clazz();
                times = 1;
                result = Notification.class;
                mockNotificationCriteria.fieldRestrictions();
                times = 1;
                result = frMap;
                mockIndexMapper.getIndexedFieldName(); times = 1;
                result = "notifierName";
                mockNotificationCriteria.numRows(); times = 1;
                result = 5;
                mockIndexMapper.mapMemberValue("status", "NEW"); times = 1;
                result = nnList;
            }
        };

        hBaseScanConfigBuilder = new HBaseScanConfigBuilder();
        hBaseScanConfigBuilder.addMappers(Notification.class, Arrays.asList(mockIndexMapper));

        Criteria<Notification> iotasEventCriteria = new CriteriaImpl<>(Notification.class);

        HBaseScanConfig<Notification> notificationScanConfig = hBaseScanConfigBuilder.getScanConfig(mockNotificationCriteria);

        System.out.println(notificationScanConfig);
        assertEquals(mockIndexMapper, notificationScanConfig.getMapper());
        assertArrayEquals("test_notifier".getBytes(CHARSET), notificationScanConfig.getStartRow());
        assertEquals(2, notificationScanConfig.filterList().getFilters().size());
        // column filter should be first
        Filter firstFilter = notificationScanConfig.filterList().getFilters().get(0);
        assertEquals(SingleColumnValueFilter.class, firstFilter.getClass());
        // page filter should be last
        Filter secondFilter = notificationScanConfig.filterList().getFilters().get(1);
        assertEquals(PageFilter.class, secondFilter.getClass());
    }
}