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
package com.hortonworks.streamline.streams.catalog.topology.component.bundle.impl;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.service.EnvironmentService;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.catalog.service.metadata.HDFSMetadataService;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
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