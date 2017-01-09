package com.hortonworks.streamline.streams.catalog;

import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.layout.component.StreamlineComponent;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;

import java.io.IOException;

public final class CatalogToLayoutConverter {
    private CatalogToLayoutConverter() {
    }

    public static TopologyLayout getTopologyLayout(Topology topology) throws IOException {
        return new TopologyLayout(topology.getId(), topology.getName(),
                topology.getConfig(), null);
    }

    public static TopologyLayout getTopologyLayout(Topology topology, TopologyDag topologyDag) throws IOException {
        return new TopologyLayout(topology.getId(), topology.getName(),
                topology.getConfig(), topologyDag);
    }

    public static com.hortonworks.streamline.streams.layout.component.Component getComponentLayout(TopologyComponent component) {
        StreamlineComponent componentLayout = new StreamlineComponent() {
            @Override
            public void accept(TopologyDagVisitor visitor) {
                throw new UnsupportedOperationException("Not intended to be called here.");
            }
        };
        componentLayout.setId(component.getId().toString());
        componentLayout.setName(component.getName());
        return componentLayout;
    }
}