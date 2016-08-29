package com.hortonworks.iotas.notification.notifiers.device;

import java.util.Map;

/**
 * This interface would be implemented to construct the custom payload for MQTTClient.
 */
public interface MQTTMessagePayloadFactory {
    /**
     * Returns the payload map, that will be actually sent as a part
     * of the MQTT publish message
     * @return
     */
    Map<String,Object> createPayloadMap(Map <String, Object> fieldsFromDeviceRegistry);
}
