package com.hortonworks.iotas.notification.notifiers.device;

import java.util.List;

public class DeviceRegistry {
    public enum Protocol {
        MQTT, COAP, AMPQ
    }

    private Long id;
    private String make;
    private String model;
    private String name;
    private String description;
    private Protocol protocol;
    private String topic;
    private String mqttBroker;
    private List<DeviceOperations> deviceOperationsList;
    private List<DeviceAttributes> deviceAttributesList;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMqttBroker() {
        return mqttBroker;
    }

    public void setMqttBroker(String mqttBroker) {
        this.mqttBroker = mqttBroker;
    }

    public List<DeviceOperations> getDeviceOperationsList() {
        return deviceOperationsList;
    }

    public void setDeviceOperationsList(List<DeviceOperations> deviceOperationsList) {
        this.deviceOperationsList = deviceOperationsList;
    }

    public List<DeviceAttributes> getDeviceAttributesList() {
        return deviceAttributesList;
    }

    public void setDeviceAttributesList(List<DeviceAttributes> deviceAttributesList) {
        this.deviceAttributesList = deviceAttributesList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceRegistry)) return false;

        DeviceRegistry that = (DeviceRegistry) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (make != null ? !make.equals(that.make) : that.make != null) return false;
        if (model != null ? !model.equals(that.model) : that.model != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (protocol != that.protocol) return false;
        if (topic != null ? !topic.equals(that.topic) : that.topic != null) return false;
        if (mqttBroker != null ? !mqttBroker.equals(that.mqttBroker) : that.mqttBroker != null) return false;
        if (deviceOperationsList != null ? !deviceOperationsList.equals(that.deviceOperationsList) : that.deviceOperationsList != null)
            return false;
        return deviceAttributesList != null ? deviceAttributesList.equals(that.deviceAttributesList) : that.deviceAttributesList == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (make != null ? make.hashCode() : 0);
        result = 31 * result + (model != null ? model.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (protocol != null ? protocol.hashCode() : 0);
        result = 31 * result + (topic != null ? topic.hashCode() : 0);
        result = 31 * result + (mqttBroker != null ? mqttBroker.hashCode() : 0);
        result = 31 * result + (deviceOperationsList != null ? deviceOperationsList.hashCode() : 0);
        result = 31 * result + (deviceAttributesList != null ? deviceAttributesList.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DeviceRegistry{" +
                "id=" + id +
                ", make='" + make + '\'' +
                ", model='" + model + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", protocol=" + protocol +
                ", topic='" + topic + '\'' +
                ", mqttBroker='" + mqttBroker + '\'' +
                ", deviceOperationsList=" + deviceOperationsList +
                ", deviceAttributesList=" + deviceAttributesList +
                '}';
    }
}
