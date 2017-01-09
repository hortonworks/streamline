package com.hortonworks.streamline.streams.layout.component;

public interface TopologyDagComponent {
    void accept(TopologyDagVisitor visitor);
}
