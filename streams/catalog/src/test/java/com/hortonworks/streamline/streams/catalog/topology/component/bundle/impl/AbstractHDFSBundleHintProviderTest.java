package org.apache.streamline.streams.catalog.topology.component.bundle.impl;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.streamline.streams.catalog.Cluster;
import org.apache.streamline.streams.catalog.service.EnvironmentService;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.catalog.service.metadata.HDFSMetadataService;
import org.apache.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import static org.junit.Assert.*;

@RunWith(JMockit.class)
public class AbstractHDFSBundleHintProviderTest {
    public static final String TEST_FIELD_NAME_FS_URL = "test";

    private class TestHDFSBundleHintProvider extends AbstractHDFSBundleHintProvider {
        @Override
        protected String getFieldNameForFSUrl() {
            return TEST_FIELD_NAME_FS_URL;
        }
    }

    private TestHDFSBundleHintProvider provider = new TestHDFSBundleHintProvider();

    @Mocked
    private EnvironmentService environmentService;

    @Mocked
    private HDFSMetadataService hdfsMetadataService;

    @Before
    public void setUp() throws Exception {
        provider.init(environmentService);
    }

    @Test
    public void getHintsOnCluster() throws Exception {
        String expectedFsUrl = "hdfs://localhost:8020";

        new Expectations() {{
           hdfsMetadataService.getDefaultFsUrl();
           result = expectedFsUrl;
        }};

        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cluster1");

        Map<String, Object> hints = provider.getHintsOnCluster(cluster);
        Assert.assertEquals(1, hints.size());
        Assert.assertEquals(expectedFsUrl, hints.get(TEST_FIELD_NAME_FS_URL));
    }

    @Test
    public void getServiceName() throws Exception {
        Assert.assertEquals(ServiceConfigurations.HDFS.name(), provider.getServiceName());
    }

}