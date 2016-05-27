package com.hortonworks.iotas.topology.component.impl;

import com.hortonworks.iotas.topology.component.IotasProcessor;
import com.hortonworks.iotas.topology.component.TopologyDagVisitor;

public class ParserProcessor extends IotasProcessor {
    @Override
    public void accept(TopologyDagVisitor visitor) {
        visitor.visit(this);
    }
}
