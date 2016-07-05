package com.hortonworks.iotas.streams.layout.component.impl;

import com.hortonworks.iotas.streams.layout.component.IotasProcessor;
import com.hortonworks.iotas.streams.layout.component.TopologyDagVisitor;

public class CustomProcessor extends IotasProcessor {
    @Override
    public void accept(TopologyDagVisitor visitor) {
        visitor.visit(this);
    }
}
