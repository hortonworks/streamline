package com.hortonworks.iotas.notification.notifiers.device;

import java.util.List;

/**
 * <p><
 * A protocol would implement this interface eg. MQTTProtocolField, CoAPProtocolField etc.
 * /p>
 */
public interface ProtocolField {
    /**
     * Returns the fields that the protocol has.
     * E.g.[QoS, DUP Flag, Retain Flag, Clean Session etc.] for MQTT
     * @return
     */
    List<String> getFields();
}
