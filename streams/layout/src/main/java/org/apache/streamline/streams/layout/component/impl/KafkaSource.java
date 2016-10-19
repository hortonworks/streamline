package org.apache.streamline.streams.layout.component.impl;

import org.apache.streamline.streams.layout.component.IotasSource;
import org.apache.streamline.streams.layout.component.TopologyDagVisitor;

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
