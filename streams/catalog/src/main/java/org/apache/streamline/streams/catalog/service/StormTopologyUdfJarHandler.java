package org.apache.streamline.streams.catalog.service;

import org.apache.streamline.streams.layout.component.Edge;
import org.apache.streamline.streams.layout.component.IotasProcessor;
import org.apache.streamline.streams.layout.component.IotasSink;
import org.apache.streamline.streams.layout.component.IotasSource;
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
    public void visit(IotasSource iotasSource) {
    }

    @Override
    public void visit(IotasSink iotasSink) {
    }

    @Override
    public void visit(IotasProcessor iotasProcessor) {
    }

    public Set<String> getUdfs() {
        return udfs;
    }
}
