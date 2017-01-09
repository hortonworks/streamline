package com.hortonworks.streamline.streams.layout.component;

import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;

public abstract class TopologyDagVisitor {
    public void visit(RulesProcessor rulesProcessor) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void visit(StreamlineSource source) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void visit(StreamlineSink sink) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void visit(StreamlineProcessor processor) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void visit(Edge edge) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
