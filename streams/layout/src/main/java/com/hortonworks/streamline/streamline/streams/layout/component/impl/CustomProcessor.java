package com.hortonworks.streamline.streams.layout.component.impl;

import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;

public class CustomProcessor extends StreamlineProcessor {
    @Override
    public void accept(TopologyDagVisitor visitor) {
        visitor.visit(this);
    }
}
