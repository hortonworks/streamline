package org.apache.streamline.streams.catalog.service.metadata.common;

import com.google.common.collect.Lists;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.streamline.streams.catalog.ServiceConfiguration;
import org.apache.streamline.streams.catalog.service.EnvironmentService;
import org.apache.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class OverrideHadoopConfigurationTest {
    private static final Logger LOG = LoggerFactory.getLogger(OverrideHadoopConfigurationTest.class);

    @Tested
    private OverrideHadoopConfiguration overrideHadoopConfiguration;
    @Mocked
    private EnvironmentService environmentService;
    @Mocked
    private ServiceConfiguration serviceConfiguration;


    /**
     * Helper class to expose the protected method {@code getProps}, used to verify if properties get correctly set
     */
    private static class HBaseConfigurationTest extends HBaseConfiguration {

        HBaseConfigurationTest(Configuration c) {
            super(c);
        }

        @Override
        public synchronized Properties getProps() {
            return super.getProps();
        }
    }

    @Test
    public void test_overrideProperties() throws Exception {
        final Properties added = new Properties() {{
            put("k1", "v11");
            put("k2", "v21");
        }};

        final Map<String, String> updated = new HashMap<String, String>() {{
            put("k1", "v12");
            put("k2", "v22");
        }};

        new Expectations() {{
            serviceConfiguration.getConfigurationMap();
            result = updated;
            times = 1;
        }};

        final HBaseConfigurationTest hbaseConfig = new HBaseConfigurationTest(HBaseConfiguration.create());
        final Properties configProps = hbaseConfig.getProps();
        LOG.debug("Initial configuration {}", configProps);

        Assert.assertFalse(configProps.containsKey("k1"));
        Assert.assertFalse(configProps.containsKey("k2"));

        configProps.putAll(added);

        Assert.assertEquals(configProps.get("k1"), "v11");
        Assert.assertEquals(configProps.get("k2"), "v21");

        OverrideHadoopConfiguration.override(environmentService, 23L, ServiceConfigurations.HBASE,
                Lists.newArrayList("hbase-site"), hbaseConfig);

        Assert.assertEquals(configProps.get("k1"), "v12");
        Assert.assertEquals(configProps.get("k2"), "v22");

        LOG.debug("Updated configuration {}", configProps);
    }

    @Test
    public void test_addProperties_nullKeyVal_notAdded() throws Exception {
        final Map<String, String> updated = new HashMap<String, String>() {{
            put(null, "val");   // null key
            put("key", null);   // null val
        }};

        new Expectations() {{
            serviceConfiguration.getConfigurationMap();
            result = updated;
            times = 1;
        }};

        final HBaseConfigurationTest hbaseConfig = new HBaseConfigurationTest(HBaseConfiguration.create());
        final Properties configProps = hbaseConfig.getProps();

        OverrideHadoopConfiguration.override(environmentService, 23L, ServiceConfigurations.HBASE,
                Lists.newArrayList("hbase-site"), hbaseConfig);

        Assert.assertFalse(configProps.containsKey("k1"));  // does not contain prop with null val
    }
}

