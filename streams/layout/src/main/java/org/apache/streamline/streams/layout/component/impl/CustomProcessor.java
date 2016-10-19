package org.apache.streamline.streams.layout.component.impl;

import org.apache.streamline.streams.layout.component.IotasProcessor;
import org.apache.streamline.streams.layout.component.TopologyDagVisitor;

public class CustomProcessor extends IotasProcessor {
    @Override
    public void accept(TopologyDagVisitor visitor) {
        visitor.visit(this);
    }
}
