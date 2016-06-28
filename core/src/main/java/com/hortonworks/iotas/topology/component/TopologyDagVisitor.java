package com.hortonworks.iotas.topology.component;

import com.hortonworks.iotas.topology.component.impl.CustomProcessor;
import com.hortonworks.iotas.topology.component.impl.HbaseSink;
import com.hortonworks.iotas.topology.component.impl.HdfsSink;
import com.hortonworks.iotas.topology.component.impl.KafkaSource;
import com.hortonworks.iotas.topology.component.impl.NotificationSink;
import com.hortonworks.iotas.topology.component.impl.ParserProcessor;
import com.hortonworks.iotas.topology.component.impl.RulesProcessor;
import com.hortonworks.iotas.topology.component.impl.normalization.NormalizationProcessor;

public abstract class TopologyDagVisitor {
    public void visit(KafkaSource kafkaSource) {
        throw new UnsupportedOperationException("Not Implemented");
    }
    public void visit(ParserProcessor parserProcessor) {
        throw new UnsupportedOperationException("Not Implemented");
    }
    public void visit(RulesProcessor rulesProcessor) {
        throw new UnsupportedOperationException("Not Implemented");
    }
    public void visit(NormalizationProcessor normalizationProcessor) {
        throw new UnsupportedOperationException("Not Implemented");
    }
    public void visit(CustomProcessor customProcessor) {
        throw new UnsupportedOperationException("Not Implemented");
    }
    public void visit(HbaseSink hbaseSink) {
        throw new UnsupportedOperationException("Not Implemented");
    }
    public void visit(HdfsSink hdfsSink) {
        throw new UnsupportedOperationException("Not Implemented");
    }
    public void visit(NotificationSink notificationSink) {
        throw new UnsupportedOperationException("Not Implemented");
    }
    public void visit(IotasComponent iotasComponent) {
        throw new UnsupportedOperationException("Not Implemented");
    }
    public void visit(Edge edge) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
