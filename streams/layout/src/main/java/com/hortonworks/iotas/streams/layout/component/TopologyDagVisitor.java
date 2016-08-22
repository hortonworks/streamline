package com.hortonworks.iotas.streams.layout.component;

import com.hortonworks.iotas.streams.layout.component.impl.EventHubSource;
import com.hortonworks.iotas.streams.layout.component.impl.HdfsSink;
import com.hortonworks.iotas.streams.layout.component.impl.CustomProcessor;
import com.hortonworks.iotas.streams.layout.component.impl.HbaseSink;
import com.hortonworks.iotas.streams.layout.component.impl.KafkaSource;
import com.hortonworks.iotas.streams.layout.component.impl.KinesisSource;
import com.hortonworks.iotas.streams.layout.component.impl.NotificationSink;
import com.hortonworks.iotas.streams.layout.component.impl.OpenTsdbSink;
import com.hortonworks.iotas.streams.layout.component.impl.ParserProcessor;
import com.hortonworks.iotas.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.iotas.streams.layout.component.impl.normalization.NormalizationProcessor;

public abstract class TopologyDagVisitor {
    public void visit(KafkaSource kafkaSource) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void visit(KinesisSource kinesisSource) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void visit(EventHubSource eventHubSource) {
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

    public void visit(OpenTsdbSink openTsdbSink) {
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
