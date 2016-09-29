package com.hortonworks.iotas.notification.notifiers.device.protocol.mqtt;

import com.hortonworks.iotas.notification.notifiers.device.protocol.Protocol;

import java.util.Map;

public class MQTTProtocol extends Protocol {
    public static final String MQTT_TOPIC = "mqttTopic";
    public static final String MQTT_BROKER_URL = "mqttBrokerURL";

    public MQTTProtocol(Map<String, Object> props) {
        super(props);
    }

    public MQTTProtocol(String topicName, String brokerUrl) {
        props.put(MQTT_TOPIC, topicName);
        props.put(MQTT_BROKER_URL, brokerUrl);
    }

    @Override
    public Name getName() {
        return Name.MQTT;
    }
}
