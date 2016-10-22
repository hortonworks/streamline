package org.apache.streamline.streams.layout.component.impl;

import org.apache.streamline.streams.layout.component.StreamlineProcessor;
import org.apache.streamline.streams.layout.component.TopologyDagVisitor;

public class CustomProcessor extends StreamlineProcessor {
    @Override
    public void accept(TopologyDagVisitor visitor) {
        visitor.visit(this);
    }
}
