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
package com.hortonworks.streamline.streams.cluster.bundle;

import com.google.common.collect.Lists;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Namespace;
import com.hortonworks.streamline.streams.catalog.NamespaceServiceClusterMapping;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RunWith(JMockit.class)
public class AbstractBundleHintProviderTest {
    public static final Long TEST_NAMESPACE_ID = 1L;
    public static final String TEST_SERVICE_NAME = "test";
    public static final Map<String, Object> TEST_HINTS = Collections.singletonMap("hello", "world");

    private class TestBundleHintProviderTest extends AbstractBundleHintProvider {
        @Override
        public Map<String, Object> getHintsOnCluster(Cluster cluster) {
            return TEST_HINTS;
        }

        @Override
        public String getServiceName() {
            return TEST_SERVICE_NAME;
        }
    }

    private AbstractBundleHintProvider provider = new TestBundleHintProviderTest();

    @Mocked
    private EnvironmentService environmentService;

    @Before
    public void setUp() throws Exception {
        provider.init(environmentService);
    }

    @Test
    public void testProvide() throws Exception {
        Cluster cluster1 = createDummyCluster(1L, "cluster1");
        Cluster cluster2 = createDummyCluster(2L, "cluster2");
        Cluster cluster3 = createDummyCluster(3L, "cluster3");

        List<NamespaceServiceClusterMapping> testServiceClusterMappings = createDummyServiceClusterMappings(
                Lists.newArrayList(cluster1, cluster2, cluster3));

        new Expectations() {{
            environmentService.getCluster(1L);
            result = cluster1;

            environmentService.getCluster(2L);
            result = cluster2;

            environmentService.getCluster(3L);
            result = cluster3;

            environmentService.listServiceClusterMapping(TEST_NAMESPACE_ID, TEST_SERVICE_NAME);
            result = testServiceClusterMappings;
        }};

        Namespace namespace = new Namespace();
        namespace.setId(TEST_NAMESPACE_ID);

        Map<Long, ComponentBundleHintProvider.BundleHintsResponse> hints = provider.provide(namespace);
        Assert.assertEquals(3, hints.size());
        Assert.assertEquals(cluster1, hints.get(cluster1.getId()).getCluster());
        Assert.assertEquals(TEST_HINTS, hints.get(cluster1.getId()).getHints());
        Assert.assertEquals(cluster2, hints.get(cluster2.getId()).getCluster());
        Assert.assertEquals(TEST_HINTS, hints.get(cluster2.getId()).getHints());
        Assert.assertEquals(cluster3, hints.get(cluster3.getId()).getCluster());
        Assert.assertEquals(TEST_HINTS, hints.get(cluster3.getId()).getHints());
    }

    private Cluster createDummyCluster(long id, String clusterName) {
        Cluster cluster = new Cluster();
        cluster.setId(id);
        cluster.setName(clusterName);
        return cluster;
    }

    private List<NamespaceServiceClusterMapping> createDummyServiceClusterMappings(List<Cluster> clusters) {
        List<NamespaceServiceClusterMapping> ret = new ArrayList<>();

        for (Cluster cluster : clusters) {
            ret.add(new NamespaceServiceClusterMapping(TEST_NAMESPACE_ID, TEST_SERVICE_NAME, cluster.getId()));
        }

        return ret;
    }

}