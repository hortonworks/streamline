package com.hortonworks.iotas.streams.layout.component;

public interface TopologyDagComponent {
    void accept(TopologyDagVisitor visitor);
}
