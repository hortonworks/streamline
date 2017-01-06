package com.hortonworks.streamline.streams.catalog.topology.component.bundle.impl;

import com.google.common.collect.Lists;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.service.EnvironmentService;
import com.hortonworks.streamline.streams.catalog.service.metadata.HBaseMetadataService;
import com.hortonworks.streamline.streams.catalog.service.metadata.common.Tables;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;

@RunWith(JMockit.class)
public class HBaseBundleHintProviderTest {

    private HBaseBundleHintProvider provider = new HBaseBundleHintProvider();

    @Mocked
    private EnvironmentService environmentService;

    @Mocked
    private HBaseMetadataService hbaseMetadataService;

    @Before
    public void setUp() throws Exception {
        provider.init(environmentService);
    }

    @Test
    public void testGetHintsOnCluster() throws Exception {
        List<String> tables = Lists.newArrayList("test1", "test2", "test3");

        new Expectations() {{
            hbaseMetadataService.getHBaseTables();
            result = new Tables(tables);
        }};

        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cluster1");

        Map<String, Object> hints = provider.getHintsOnCluster(cluster);
        Assert.assertNotNull(hints);
        Assert.assertEquals(1, hints.size());
        Assert.assertEquals(tables, hints.get(HBaseBundleHintProvider.FIELD_NAME_TABLE));

        new Verifications() {{
            hbaseMetadataService.getHBaseTables();
        }};
    }

    @Test
    public void testGetServiceName() throws Exception {
        Assert.assertEquals(ServiceConfigurations.HBASE.name(), provider.getServiceName());
    }

}