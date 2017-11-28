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
package com.hortonworks.streamline.streams.cluster.service;

import com.google.common.collect.Lists;
import com.hortonworks.registries.common.QueryParam;
import com.hortonworks.registries.storage.StorageManager;
import com.hortonworks.registries.storage.TransactionManager;
import com.hortonworks.streamline.streams.cluster.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.catalog.NamespaceServiceClusterMap;
import com.hortonworks.streamline.streams.cluster.ClusterImporter;
import com.hortonworks.streamline.streams.cluster.container.ContainingNamespaceAwareContainer;
import com.hortonworks.streamline.streams.cluster.discovery.ServiceNodeDiscoverer;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for EnvironmentService.
 */
@RunWith(JMockit.class)
public class EnvironmentServiceTest {

    public static final String NAMESPACE_SERVICE_CLUSTER_MAP = "namespace_service_cluster_map";
    @Tested
    private EnvironmentService environmentService;

    @Injectable
    private StorageManager dao;

    @Injectable
    private ClusterImporter clusterImporter;

    @Injectable
    private TransactionManager transactionManager;

    @Injectable
    private List<ContainingNamespaceAwareContainer> containers;

    @Mocked
    private ServiceNodeDiscoverer discoverer;

    class MockedNamespaceAwareContainer implements ContainingNamespaceAwareContainer {
        private List<Long> invalidatedNamespaceIds = new ArrayList<>();

        @Override
        public void invalidateInstance(Long namespaceId) {
            invalidatedNamespaceIds.add(namespaceId);
        }

        public List<Long> getInvalidatedNamespaceIds() {
            return invalidatedNamespaceIds;
        }
    }

    @Test
    public void invalidateContainersWhenImportingClusterServices() throws Exception {
        Deencapsulation.setField(environmentService, "clusterImporter", clusterImporter);

        long clusterId = 1L;
        Cluster testCluster = new Cluster();
        testCluster.setId(clusterId);

        ArrayList<Long> namespaceIds = Lists.newArrayList(1L, 2L, 3L);
        List<NamespaceServiceClusterMap> mappings = new ArrayList<>();
        mappings.add(new NamespaceServiceClusterMap(1L, "STORM", clusterId));
        mappings.add(new NamespaceServiceClusterMap(2L, "KAFKA", clusterId));
        mappings.add(new NamespaceServiceClusterMap(3L, "HADOOP", clusterId));

        MockedNamespaceAwareContainer container1 = new MockedNamespaceAwareContainer();
        MockedNamespaceAwareContainer container2 = new MockedNamespaceAwareContainer();
        environmentService.addNamespaceAwareContainer(container1);
        environmentService.addNamespaceAwareContainer(container2);

        new Expectations() {{
           clusterImporter.importCluster(discoverer, testCluster);
           result = testCluster;

           dao.find(NAMESPACE_SERVICE_CLUSTER_MAP,
                   Collections.singletonList(new QueryParam("clusterId", String.valueOf(clusterId))));
           result = mappings;
        }};

        // we're just checking whether it calls invalidation to associated containers properly
        environmentService.importClusterServices(discoverer, testCluster);

        assertEquals(3, container1.getInvalidatedNamespaceIds().size());
        assertTrue(container1.getInvalidatedNamespaceIds().containsAll(namespaceIds));
        assertEquals(3, container2.getInvalidatedNamespaceIds().size());
        assertTrue(container2.getInvalidatedNamespaceIds().containsAll(namespaceIds));
    }
}