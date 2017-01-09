package org.apache.streamline.streams.catalog.service;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.apache.streamline.common.util.FileStorage;
import org.apache.streamline.registries.model.client.MLModelRegistryClient;
import org.apache.streamline.storage.StorableKey;
import org.apache.streamline.storage.StorageManager;
import org.apache.streamline.streams.catalog.Topology;
import org.apache.streamline.streams.catalog.TopologyVersionInfo;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JMockit.class)
public class StreamCatalogServiceTest {

    @Tested
    private StreamCatalogService streamCatalogService;

    @Injectable
    private StorageManager dao;

    @Injectable
    private FileStorage fileStorage;

    @Injectable
    private MLModelRegistryClient modelRegistryClient;

    @Injectable
    private Map<String, Object> configuration;

    @Test
    public void testListTopologiesProvidesVersionedTimestamp_STREAMLINE_526() {
        List<Topology> topologies = new ArrayList<>();
        topologies.add(createTopology(1L));
        topologies.add(createTopology(2L));
        topologies.add(createTopology(3L));

        List<TopologyVersionInfo> versions = topologies.stream()
                .map(x -> createTopologyVersionInfo(x.getId(), x.getId()))
                .collect(Collectors.toList());

        new Expectations() {{
            dao.find(withEqual(new Topology().getNameSpace()), withAny(new ArrayList<>()));
            result = topologies;

            dao.find(withEqual(new TopologyVersionInfo().getNameSpace()), withAny(new ArrayList<>()));
            result = versions;

            dao.get(withEqual(new StorableKey(versions.get(0).getNameSpace(), versions.get(0).getPrimaryKey())));
            result = versions.get(0);

            dao.get(withEqual(new StorableKey(versions.get(1).getNameSpace(), versions.get(1).getPrimaryKey())));
            result = versions.get(1);

            dao.get(withEqual(new StorableKey(versions.get(2).getNameSpace(), versions.get(2).getPrimaryKey())));
            result = versions.get(2);
        }};

        Collection<Topology> result = streamCatalogService.listTopologies();
        assertTrue(result.size() > 0);
        assertFalse(result.stream().anyMatch(x -> x.getVersionTimestamp() == null));
    }

    private Topology createTopology(long id) {
        Topology topology = new Topology();
        topology.setId(id);
        topology.setName("name" + id);
        topology.setVersionId(id);
        // not set timestamp
        return topology;
    }

    private TopologyVersionInfo createTopologyVersionInfo(Long id, Long topologyId) {
        TopologyVersionInfo topologyVersionInfo = new TopologyVersionInfo();
        topologyVersionInfo.setId(id);
        topologyVersionInfo.setName("name" + id);
        topologyVersionInfo.setTopologyId(topologyId);
        topologyVersionInfo.setDescription("description" + id);
        topologyVersionInfo.setTimestamp(System.currentTimeMillis());
        return topologyVersionInfo;
    }

}