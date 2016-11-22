package org.apache.streamline.streams.catalog.topology.component;


import org.apache.streamline.common.QueryParam;
import org.apache.streamline.streams.catalog.Topology;
import org.apache.streamline.streams.catalog.TopologyComponent;
import org.apache.streamline.streams.catalog.TopologyEdge;
import org.apache.streamline.streams.catalog.TopologyProcessor;
import org.apache.streamline.streams.catalog.TopologySink;
import org.apache.streamline.streams.catalog.TopologySource;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.layout.component.TopologyDag;

import java.util.ArrayList;
import java.util.List;

public class TopologyDagBuilder {
    private final StreamCatalogService catalogService;
    private final TopologyComponentFactory factory;

    public TopologyDagBuilder(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
        factory = new TopologyComponentFactory(catalogService);
    }

    public TopologyDag getDag(Topology topology) {
        try {
            TopologyDag dag = new TopologyDag();
            addSources(dag, topology);
            addProcessors(dag, topology);
            addSinks(dag, topology);
            addEdges(dag, topology);
            return dag;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void addSources(TopologyDag dag, Topology topology) throws Exception {
        for (TopologySource topologySource : catalogService.listTopologySources(queryParam(topology))) {
            dag.add(factory.getStreamlineSource(topologySource));
        }
    }

    private void addProcessors(TopologyDag dag, Topology topology) throws Exception {
        for (TopologyProcessor topologyProcessor : catalogService.listTopologyProcessors(queryParam(topology))) {
            dag.add(factory.getStreamlineProcessor(topologyProcessor));
        }
    }

    private void addSinks(TopologyDag dag, Topology topology) throws Exception {
        for (TopologySink topologySink : catalogService.listTopologySinks(queryParam(topology))) {
            dag.add(factory.getStreamlineSink(topologySink));
        }
    }

    private void addEdges(TopologyDag dag, Topology topology) throws Exception {
        for (TopologyEdge topologyEdge: catalogService.listTopologyEdges(queryParam(topology))) {
            dag.addEdge(factory.getStreamlineEdge(topologyEdge));
        }
    }

    private List<QueryParam> queryParam(Topology topology) {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam(TopologyComponent.TOPOLOGYID, topology.getId().toString()));
        queryParams.add(new QueryParam(TopologyComponent.VERSIONID, topology.getVersionId().toString()));
        return queryParams;
    }
}
