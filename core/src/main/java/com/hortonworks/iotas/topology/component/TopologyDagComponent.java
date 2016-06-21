package com.hortonworks.iotas.topology.component;

public interface TopologyDagComponent {
    void accept(TopologyDagVisitor visitor);
}
