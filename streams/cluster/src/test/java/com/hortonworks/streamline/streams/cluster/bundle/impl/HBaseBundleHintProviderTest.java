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
package com.hortonworks.streamline.streams.cluster.bundle.impl;

import com.google.common.collect.Lists;

import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.cluster.service.metadata.HBaseMetadataService;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Tables;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

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