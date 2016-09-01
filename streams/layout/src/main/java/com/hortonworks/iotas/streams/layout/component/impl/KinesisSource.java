package com.hortonworks.iotas.streams.layout.component.impl;

import com.hortonworks.iotas.streams.layout.component.IotasSource;
import com.hortonworks.iotas.streams.layout.component.TopologyDagVisitor;

public class KinesisSource extends IotasSource {
    @Override
    public void accept(TopologyDagVisitor visitor) {
        visitor.visit(this);
    }
}
