package org.apache.streamline.streams.catalog.service;

import org.apache.streamline.streams.layout.component.Edge;
import org.apache.streamline.streams.layout.component.StreamlineProcessor;
import org.apache.streamline.streams.layout.component.StreamlineSink;
import org.apache.streamline.streams.layout.component.StreamlineSource;
import org.apache.streamline.streams.layout.component.TopologyDagVisitor;
import org.apache.streamline.streams.layout.component.impl.RulesProcessor;
import org.apache.streamline.streams.layout.component.rule.Rule;

import java.util.HashSet;
import java.util.Set;

public class StormTopologyUdfJarHandler extends TopologyDagVisitor {
    private final Set<String> udfs = new HashSet<>();

    @Override
    public void visit(RulesProcessor rulesProcessor) {
        for (Rule rule : rulesProcessor.getRules()) {
            udfs.addAll(rule.getReferredUdfs());
        }
    }

    @Override
    public void visit(Edge edge) {
    }

    @Override
    public void visit(StreamlineSource source) {
    }

    @Override
    public void visit(StreamlineSink sink) {
    }

    @Override
    public void visit(StreamlineProcessor processor) {
    }

    public Set<String> getUdfs() {
        return udfs;
    }
}
