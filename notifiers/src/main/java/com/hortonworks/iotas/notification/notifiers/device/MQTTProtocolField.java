package com.hortonworks.iotas.notification.notifiers.device;

import java.util.ArrayList;
import java.util.List;

public class MQTTProtocolField implements ProtocolField{

    private String mqttTopic; //The topic to which the producer publishes the data
    private String mqttBrokerURL; //The URL of the MQTT broker
    private String fields[] = {"mqttTopic", "mqttBrokerURL"};


    //for jackson
    public MQTTProtocolField() {
    }

    public MQTTProtocolField(String mqttTopic, String mqttBrokerURL) {
        this.mqttTopic = mqttTopic;
        this.mqttBrokerURL = mqttBrokerURL;
    }

    public String getMqttTopic() {
        return mqttTopic;
    }

    public void setMqttTopic(String mqttTopic) {
        this.mqttTopic = mqttTopic;
    }

    public String getMqttBrokerURL() {
        return mqttBrokerURL;
    }

    public void setMqttBrokerURL(String mqttBrokerURL) {
        this.mqttBrokerURL = mqttBrokerURL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MQTTProtocolField)) return false;

        MQTTProtocolField that = (MQTTProtocolField) o;

        if (mqttTopic != null ? !mqttTopic.equals(that.mqttTopic) : that.mqttTopic != null) return false;
        return mqttBrokerURL != null ? mqttBrokerURL.equals(that.mqttBrokerURL) : that.mqttBrokerURL == null;

    }

    @Override
    public int hashCode() {
        int result = mqttTopic != null ? mqttTopic.hashCode() : 0;
        result = 31 * result + (mqttBrokerURL != null ? mqttBrokerURL.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MQTTProtocolField{" +
                "mqttTopic='" + mqttTopic + '\'' +
                ", mqttBrokerURL='" + mqttBrokerURL + '\'' +
                '}';
    }

    @Override
    public List<String> getFields() {
        List<String> fields = new ArrayList<>();
        for(String field: this.fields) {
            fields.add(field);
        }
        return fields;
    }
}
