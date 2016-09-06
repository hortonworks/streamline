package com.hortonworks.iotas.notification.notifiers.device.protocol;

import java.util.HashMap;
import java.util.Map;

public abstract class Protocol {
    public static final String PROTOCOL_NAME = "name";

    public enum Name {
        MQTT,
        COAP,
        AMPQ
    }

    protected Map<String, Object> props = new HashMap<>();

    public Protocol() {
    }

    public Protocol(Map<String, Object> props) {
        this.props = props;
    }

    public Map<String, Object> getProperties() {
        return props;
    }

    public abstract Name getName();
}
