package com.hortonworks.iotas.streams.layout.component.impl;

import com.hortonworks.iotas.streams.layout.component.IotasSink;
import com.hortonworks.iotas.streams.layout.component.TopologyDagVisitor;

public class HbaseSink extends IotasSink {
    @Override
    public void accept(TopologyDagVisitor visitor) {
        visitor.visit(this);
    }
}
