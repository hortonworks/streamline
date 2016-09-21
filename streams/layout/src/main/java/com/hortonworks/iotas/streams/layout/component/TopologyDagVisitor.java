package com.hortonworks.iotas.streams.layout.component;

import com.hortonworks.iotas.streams.layout.component.impl.RulesProcessor;

public abstract class TopologyDagVisitor {
    public void visit(RulesProcessor rulesProcessor) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void visit(IotasSource iotasSource) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void visit(IotasSink iotasSink) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void visit(IotasProcessor iotasProcessor) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void visit(Edge edge) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
