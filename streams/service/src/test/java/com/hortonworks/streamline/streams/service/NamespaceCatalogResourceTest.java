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
package com.hortonworks.streamline.streams.service;

import com.google.common.collect.Lists;
import com.hortonworks.streamline.streams.cluster.catalog.NamespaceServiceClusterMap;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import com.hortonworks.streamline.streams.security.impl.NoopAuthorizer;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import com.hortonworks.streamline.common.exception.service.exception.request.BadRequestException;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.exception.TopologyNotAliveException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

@RunWith(JMockit.class)
public class NamespaceCatalogResourceTest {
    public static final String TEST_STREAMING_ENGINE = "STORM";
    public static final String TEST_TIME_SERIES_DB = "AMBARI_METRICS";

    @Tested
    private NamespaceCatalogResource namespaceCatalogResource;

    @Injectable
    private StreamlineAuthorizer authorizer = new NoopAuthorizer();

    @Injectable
    private StreamCatalogService catalogService;

    @Injectable
    private TopologyActionsService topologyActionsService;

    @Injectable
    private EnvironmentService environmentService;

    @Injectable
    private SecurityContext securityContext;

    @Test
    public void testExcludeStreamingEngineViaSetServicesToClusterInNamespace() throws Exception {
        Long testNamespaceId = 1L;

        Namespace testNamespace = createTestNamespace(testNamespaceId, TEST_STREAMING_ENGINE, TEST_TIME_SERIES_DB);

        Collection<NamespaceServiceClusterMap> existingMappings = createTestMappingsForExisting(testNamespaceId);

        setupExpectationForSimulatingTopologyIsRunning(testNamespaceId, testNamespace, existingMappings);

        List<NamespaceServiceClusterMap> mappingsToApply = existingMappings.stream()
                .filter(m -> !m.getServiceName().equals(TEST_STREAMING_ENGINE)).collect(toList());

        try {
            namespaceCatalogResource.setServicesToClusterInNamespace(testNamespaceId, mappingsToApply, securityContext);
            Assert.fail("Should throw BadRequestException");
        } catch (BadRequestException e) {
            // passed
        }

        new Verifications() {{
            // request fails before removing existing mappings
            environmentService.removeServiceClusterMapping(testNamespaceId, anyString, anyLong);
            times = 0;
        }};
    }

    @Test
    public void testChangeMappingOfStreamingEngineViaSetServicesToClusterInNamespace() throws Exception {
        Long testNamespaceId = 1L;

        Namespace testNamespace = createTestNamespace(testNamespaceId, TEST_STREAMING_ENGINE, TEST_TIME_SERIES_DB);

        Collection<NamespaceServiceClusterMap> existingMappings = createTestMappingsForExisting(testNamespaceId);

        setupExpectationForSimulatingTopologyIsRunning(testNamespaceId, testNamespace, existingMappings);

        List<NamespaceServiceClusterMap> mappingsToApply = existingMappings.stream()
                .filter(m -> !m.getServiceName().equals(TEST_STREAMING_ENGINE)).collect(toList());
        // change the mapping of streaming engine to cluster id 2
        mappingsToApply.add(new NamespaceServiceClusterMap(testNamespaceId, TEST_STREAMING_ENGINE, 2L));

        try {
            namespaceCatalogResource.setServicesToClusterInNamespace(testNamespaceId, mappingsToApply, securityContext);
            Assert.fail("Should throw BadRequestException");
        } catch (BadRequestException e) {
            // passed
        }

        new Verifications() {{
            // request fails before removing existing mappings
            environmentService.removeServiceClusterMapping(testNamespaceId, anyString, anyLong);
            times = 0;
        }};
    }

    @Test
    public void testOverwriteExistingStreamingEngineMappingViaSetServicesToClusterInNamespace() throws Exception {
        Long testNamespaceId = 1L;

        Namespace testNamespace = createTestNamespace(testNamespaceId, TEST_STREAMING_ENGINE, TEST_TIME_SERIES_DB);

        Collection<NamespaceServiceClusterMap> existingMappings = createTestMappingsForExisting(testNamespaceId);

        new Expectations() {{
            environmentService.getNamespace(testNamespaceId);
            result = testNamespace;
            environmentService.listServiceClusterMapping(testNamespaceId);
            result = existingMappings;
        }};

        namespaceCatalogResource.setServicesToClusterInNamespace(testNamespaceId, new ArrayList<>(existingMappings), securityContext);

        new Verifications() {{
            catalogService.listTopologies();
            times = 0;
            topologyActionsService.getRuntimeTopologyId(withAny(new Topology()), anyString);
            times = 0;
            environmentService.removeServiceClusterMapping(testNamespaceId, anyString, anyLong);
            times = existingMappings.size();
            environmentService.addOrUpdateServiceClusterMapping(withAny(new NamespaceServiceClusterMap()));
            times = existingMappings.size();
        }};
    }

    @Test
    public void testMappingMultipleStreamingEngineViaSetServicesToClusterInNamespace() throws Exception {
        Long testNamespaceId = 1L;

        Namespace testNamespace = createTestNamespace(testNamespaceId, TEST_STREAMING_ENGINE, TEST_TIME_SERIES_DB);

        Collection<NamespaceServiceClusterMap> existingMappings = createTestMappingsForExisting(testNamespaceId);

        new Expectations() {{
            environmentService.getNamespace(testNamespaceId);
            result = testNamespace;
            environmentService.listServiceClusterMapping(testNamespaceId);
            result = existingMappings;
        }};

        List<NamespaceServiceClusterMap> mappingsToApply = Lists.newArrayList(
                new NamespaceServiceClusterMap(testNamespaceId, TEST_STREAMING_ENGINE, 1L),
                new NamespaceServiceClusterMap(testNamespaceId, TEST_STREAMING_ENGINE, 2L),
                new NamespaceServiceClusterMap(testNamespaceId, TEST_TIME_SERIES_DB, 1L),
                new NamespaceServiceClusterMap(testNamespaceId, "KAFKA", 1L)
        );

        try {
            namespaceCatalogResource.setServicesToClusterInNamespace(testNamespaceId, mappingsToApply, securityContext);
            Assert.fail("Should throw BadRequestException");
        } catch (BadRequestException e) {
            // passed
        }

        new Verifications() {{
            catalogService.listTopologies();
            times = 0;
            topologyActionsService.getRuntimeTopologyId(withAny(new Topology()), anyString);
            times = 0;
            // request fails before removing existing mappings
            environmentService.removeServiceClusterMapping(testNamespaceId, anyString, anyLong);
            times = 0;
        }};
    }

    @Test
    public void testMappingMultipleTimeSeriesDBViaSetServicesToClusterInNamespace() throws Exception {
        Long testNamespaceId = 1L;

        Namespace testNamespace = createTestNamespace(testNamespaceId, TEST_STREAMING_ENGINE, TEST_TIME_SERIES_DB);

        Collection<NamespaceServiceClusterMap> existingMappings = createTestMappingsForExisting(testNamespaceId);

        new Expectations() {{
            environmentService.getNamespace(testNamespaceId);
            result = testNamespace;
            environmentService.listServiceClusterMapping(testNamespaceId);
            result = existingMappings;
        }};

        List<NamespaceServiceClusterMap> mappingsToApply = Lists.newArrayList(
                new NamespaceServiceClusterMap(testNamespaceId, TEST_STREAMING_ENGINE, 1L),
                new NamespaceServiceClusterMap(testNamespaceId, TEST_TIME_SERIES_DB, 1L),
                new NamespaceServiceClusterMap(testNamespaceId, TEST_TIME_SERIES_DB, 2L),
                new NamespaceServiceClusterMap(testNamespaceId, "KAFKA", 1L)
        );

        try {
            namespaceCatalogResource.setServicesToClusterInNamespace(testNamespaceId, mappingsToApply, securityContext);
            Assert.fail("Should throw BadRequestException");
        } catch (BadRequestException e) {
            // passed
        }

        new Verifications() {{
            catalogService.listTopologies();
            times = 0;
            topologyActionsService.getRuntimeTopologyId(withAny(new Topology()), anyString);
            times = 0;
            // request fails before removing existing mappings
            environmentService.removeServiceClusterMapping(testNamespaceId, anyString, anyLong);
            times = 0;
        }};
    }

    @Test
    public void testAddStreamingEngineWhenStreamingEngineAlreadyExistsViaMapServiceToClusterInNamespace() throws Exception {
        Long testNamespaceId = 1L;

        Namespace testNamespace = createTestNamespace(testNamespaceId, TEST_STREAMING_ENGINE, TEST_TIME_SERIES_DB);

        Collection<NamespaceServiceClusterMap> existingMappings = createTestMappingsForExisting(testNamespaceId);

        new Expectations() {{
            environmentService.getNamespace(testNamespaceId);
            result = testNamespace;
            environmentService.listServiceClusterMapping(testNamespaceId);
            result = existingMappings;
        }};

        NamespaceServiceClusterMap newMapping = new NamespaceServiceClusterMap(testNamespaceId, TEST_STREAMING_ENGINE, 2L);

        try {
            namespaceCatalogResource.mapServiceToClusterInNamespace(testNamespaceId, newMapping, securityContext);
            Assert.fail("Should throw BadRequestException");
        } catch (BadRequestException e) {
            // passed
        }

        new Verifications() {{
            environmentService.addOrUpdateServiceClusterMapping(withAny(new NamespaceServiceClusterMap()));
            times = 0;
        }};
    }

    @Test
    public void testAddTimeSeriesDBWhenTimeSeriesDBAlreadyExistsViaMapServiceToClusterInNamespace() throws Exception {
        Long testNamespaceId = 1L;

        Namespace testNamespace = createTestNamespace(testNamespaceId, TEST_STREAMING_ENGINE, TEST_TIME_SERIES_DB);

        Collection<NamespaceServiceClusterMap> existingMappings = createTestMappingsForExisting(testNamespaceId);

        new Expectations() {{
            environmentService.getNamespace(testNamespaceId);
            result = testNamespace;
            environmentService.listServiceClusterMapping(testNamespaceId);
            result = existingMappings;
        }};

        NamespaceServiceClusterMap newMapping = new NamespaceServiceClusterMap(testNamespaceId, TEST_TIME_SERIES_DB, 2L);

        try {
            namespaceCatalogResource.mapServiceToClusterInNamespace(testNamespaceId, newMapping, securityContext);
            Assert.fail("Should throw BadRequestException");
        } catch (BadRequestException e) {
            // passed
        }

        new Verifications() {{
            environmentService.addOrUpdateServiceClusterMapping(withAny(new NamespaceServiceClusterMap()));
            times = 0;
        }};
    }

    @Test
    public void testOverwriteSameStreamingEngineMappingViaMapServiceToClusterInNamespace() throws Exception {
        Long testNamespaceId = 1L;

        Namespace testNamespace = createTestNamespace(testNamespaceId, TEST_STREAMING_ENGINE, TEST_TIME_SERIES_DB);

        Collection<NamespaceServiceClusterMap> existingMappings = createTestMappingsForExisting(testNamespaceId);

        new Expectations() {{
            environmentService.getNamespace(testNamespaceId);
            result = testNamespace;
            environmentService.listServiceClusterMapping(testNamespaceId);
            result = existingMappings;
        }};

        NamespaceServiceClusterMap existingStreamingEngineMapping = existingMappings.stream()
                .filter(m -> m.getServiceName().equals(TEST_STREAMING_ENGINE)).findAny().get();

        namespaceCatalogResource.mapServiceToClusterInNamespace(testNamespaceId, existingStreamingEngineMapping, securityContext);

        new Verifications() {{
            environmentService.addOrUpdateServiceClusterMapping(withAny(new NamespaceServiceClusterMap()));
            times = 1;
        }};
    }

    @Test
    public void testOverwriteSameTimeSeriesDBMappingViaMapServiceToClusterInNamespace() throws Exception {
        Long testNamespaceId = 1L;

        Namespace testNamespace = createTestNamespace(testNamespaceId, TEST_STREAMING_ENGINE, TEST_TIME_SERIES_DB);

        Collection<NamespaceServiceClusterMap> existingMappings = createTestMappingsForExisting(testNamespaceId);

        new Expectations() {{
            environmentService.getNamespace(testNamespaceId);
            result = testNamespace;
            environmentService.listServiceClusterMapping(testNamespaceId);
            result = existingMappings;
        }};

        NamespaceServiceClusterMap existingTimeSeriesDBMapping = existingMappings.stream()
                .filter(m -> m.getServiceName().equals(TEST_TIME_SERIES_DB)).findAny().get();

        namespaceCatalogResource.mapServiceToClusterInNamespace(testNamespaceId, existingTimeSeriesDBMapping, securityContext);

        new Verifications() {{
            environmentService.addOrUpdateServiceClusterMapping(withAny(new NamespaceServiceClusterMap()));
            times = 1;
        }};
    }

    @Test
    public void testUnmapStreamingEngineWhenTopologyIsRunningViaUnmapServiceToClusterInNamespace() throws Exception {
        Long testNamespaceId = 1L;

        Namespace testNamespace = createTestNamespace(testNamespaceId, TEST_STREAMING_ENGINE, TEST_TIME_SERIES_DB);

        Collection<NamespaceServiceClusterMap> existingMappings = createTestMappingsForExisting(testNamespaceId);

        setupExpectationForSimulatingTopologyIsRunning(testNamespaceId, testNamespace, existingMappings);

        try {
            namespaceCatalogResource.unmapServiceToClusterInNamespace(testNamespaceId, TEST_STREAMING_ENGINE, 1L, securityContext);
            Assert.fail("Should throw BadRequestException");
        } catch (BadRequestException e) {
            // passed
        }

        new Verifications() {{
            environmentService.removeServiceClusterMapping(testNamespaceId, TEST_STREAMING_ENGINE, 1L);
            times = 0;
        }};
    }

    @Test
    public void testUnmapStreamingEngineWhenTopologyIsRunningViaUnmapAllServiceToClusterInNamespace() throws Exception {
        Long testNamespaceId = 1L;

        Namespace testNamespace = createTestNamespace(testNamespaceId, TEST_STREAMING_ENGINE, TEST_TIME_SERIES_DB);

        Collection<NamespaceServiceClusterMap> existingMappings = createTestMappingsForExisting(testNamespaceId);

        setupExpectationForSimulatingTopologyIsRunning(testNamespaceId, testNamespace, existingMappings);

        try {
            namespaceCatalogResource.unmapAllServicesToClusterInNamespace(testNamespaceId, securityContext);
            Assert.fail("Should throw BadRequestException");
        } catch (BadRequestException e) {
            // passed
        }

        new Verifications() {{
            environmentService.removeServiceClusterMapping(testNamespaceId, anyString, anyLong);
            times = 0;
        }};
    }

    private List<Topology> createTestTopologies(Long testNamespaceId) {
        Topology topology1 = new Topology();
        topology1.setId(1L);
        topology1.setName("test-topology-1");
        topology1.setNamespaceId(testNamespaceId);
        topology1.setVersionId(1L);

        Topology topology2 = new Topology();
        topology2.setId(2L);
        topology2.setName("test-topology-2");
        topology2.setNamespaceId(testNamespaceId);
        topology2.setVersionId(1L);

        return Lists.newArrayList(topology1, topology2);
    }

    private Namespace createTestNamespace(Long namespaceId, String streamingEngine, String timeSeriesDB) {
        Namespace testNamespace = new Namespace();
        testNamespace.setId(namespaceId);
        testNamespace.setName("test-namespace");
        testNamespace.setStreamingEngine(streamingEngine);
        testNamespace.setTimeSeriesDB(timeSeriesDB);
        return testNamespace;
    }

    private Collection<NamespaceServiceClusterMap> createTestMappingsForExisting(Long testNamespaceId) {
        return Lists.newArrayList(
                new NamespaceServiceClusterMap(testNamespaceId, TEST_STREAMING_ENGINE, 1L),
                new NamespaceServiceClusterMap(testNamespaceId, TEST_TIME_SERIES_DB, 1L),
                new NamespaceServiceClusterMap(testNamespaceId, "KAFKA", 1L),
                new NamespaceServiceClusterMap(testNamespaceId, "ZOOKEEPER", 1L)
        );
    }

    private void setupExpectationForSimulatingTopologyIsRunning(final Long testNamespaceId, final Namespace testNamespace,
                                                                final Collection<NamespaceServiceClusterMap> existingMappings) throws IOException {
        List<Topology> topologies = createTestTopologies(testNamespaceId);
        new Expectations() {{
            environmentService.getNamespace(testNamespaceId);
            result = testNamespace;
            environmentService.listServiceClusterMapping(testNamespaceId);
            result = existingMappings;
            catalogService.listTopologies();
            result = topologies;
            // assuming first topology is not running
            topologyActionsService.getRuntimeTopologyId(topologies.get(0), anyString);
            result = new TopologyNotAliveException("generated exception for purpose");
            // and second topology is running now
            topologyActionsService.getRuntimeTopologyId(topologies.get(1), anyString);
            result = "dummy-storm-topology-id";
        }};
    }
}