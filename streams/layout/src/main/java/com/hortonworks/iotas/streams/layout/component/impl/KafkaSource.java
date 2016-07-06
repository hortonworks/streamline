package com.hortonworks.iotas.streams.layout.component.impl;

import com.hortonworks.iotas.streams.layout.component.IotasSource;
import com.hortonworks.iotas.streams.layout.component.TopologyDagVisitor;

public class KafkaSource extends IotasSource {
    private static final String ZK_URL = "zkUrl";

    public KafkaSource() {}

    public String getZkUrl() {
        return getConfig().get(ZK_URL);
    }

    @Override
    public void accept(TopologyDagVisitor visitor) {
        visitor.visit(this);
    }
}
