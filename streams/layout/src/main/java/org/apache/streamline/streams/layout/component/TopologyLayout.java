package org.apache.streamline.streams.layout.component;

public class TopologyLayout {
    private final Long id;
    private final String name;
    private final String config;
    private final TopologyDag topologyDag;

    public TopologyLayout(Long id, String name, String config, TopologyDag topologyDag) {
        this.id = id;
        this.name = name;
        this.config = config;
        this.topologyDag = topologyDag;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getConfig() {
        return config;
    }

    public TopologyDag getTopologyDag() {
        return topologyDag;
    }
}
