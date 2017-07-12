/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.streams.cluster.service.metadata.common;

import com.google.common.collect.Lists;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.cluster.service.metadata.HBaseMetadataService;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import static com.hortonworks.streamline.streams.cluster.service.metadata.HBaseMetadataService.SAM_MAX_HBASE_CLIENT_RETRIES_NUMBER;
import static org.apache.hadoop.hbase.HConstants.HBASE_CLIENT_RETRIES_NUMBER;

@RunWith(JMockit.class)
public class OverrideHadoopConfigurationTest {
    private static final Logger LOG = LoggerFactory.getLogger(OverrideHadoopConfigurationTest.class);
    private static final int DEFAULT_HBASE_CLIENT_RETRIES_NUMBER = 35;

    @Mocked
    private EnvironmentService environmentService;
    @Mocked
    private ServiceConfiguration serviceConfiguration;


    /**
     * Helper class to expose the protected method {@code getProps}, used to verify if properties get correctly set
     */
    private static class HBaseConfigurationTest extends HBaseConfiguration {
        HBaseConfigurationTest(Configuration config) {
            super(config);
        }

        static HBaseConfigurationTest newInstance() {
            return new HBaseConfigurationTest(HBaseConfiguration.create());
        }

        @Override
        protected synchronized Properties getProps() {
            return super.getProps();
        }
    }

    @Test
    public void test_overridePropHBaseClientRetriesNum_lessThanSamMax_lessThanSamMax() throws Exception {
        test_overrideProp(HBASE_CLIENT_RETRIES_NUMBER,
            SAM_MAX_HBASE_CLIENT_RETRIES_NUMBER - 1,
            SAM_MAX_HBASE_CLIENT_RETRIES_NUMBER - 1);
    }

    @Test
    public void test_overridePropHBaseClientRetriesNum_greaterThanSamMax_SamMax() throws Exception {
        test_overrideProp(HBASE_CLIENT_RETRIES_NUMBER,
            DEFAULT_HBASE_CLIENT_RETRIES_NUMBER + 1,
                            SAM_MAX_HBASE_CLIENT_RETRIES_NUMBER);
    }

    @Test
    public void test_overrideNonExistingProp_someVal_someVal() throws Exception {
        final int newPropVal = 10;
        test_overrideProp("new.prop", newPropVal, newPropVal);
    }

    @Test
    public void test_addProperty_nullKeyVal_notAdded() throws Exception {
        final HBaseConfigurationTest initialConfig = HBaseConfigurationTest.newInstance();
        final Map<String, String> updatedConfig = new HashMap<String, String>() {{
            put(null, "val");   // null key
            put("key", null);   // null val
            put(null, null);
        }};
        new ConfigExpectations(updatedConfig);

        doOverride(initialConfig, Collections.emptyMap());

        // validates it does not contain prop with null val
        Assert.assertFalse(initialConfig.getProps().containsKey("key"));
    }

    private void test_overrideProp(final String propName, final int actualInitialVal, final int expectedOverriddenVal) throws Exception {
        final HBaseConfigurationTest initialConfig = HBaseConfigurationTest.newInstance();
        initialConfig.getProps().setProperty(propName, String.valueOf(actualInitialVal));
        Assert.assertEquals(String.valueOf(actualInitialVal), initialConfig.get(propName));

        final Map<String, String> updatedConfig = new HashMap<String, String>() {{
            put(propName, String.valueOf(actualInitialVal));
        }};
        new ConfigExpectations(updatedConfig);

        doOverride(initialConfig, HBaseMetadataService.getMaxHBaseClientRetries());

        Assert.assertEquals(String.valueOf(expectedOverriddenVal), initialConfig.get(propName));
    }

    private void doOverride(HBaseConfigurationTest initialConfig, Map<String, Function<String, String>> samOverrides) throws Exception {
        OverrideHadoopConfiguration.override(environmentService, 23L, ServiceConfigurations.HBASE,
            Lists.newArrayList("hbase-site"), initialConfig, samOverrides);
    }

    private final class ConfigExpectations extends Expectations {
        ConfigExpectations(Map<String, String> config) throws IOException {
            super();
            serviceConfiguration.getConfigurationMap();
            result = config;
            times = 1;
        }
    }
}

