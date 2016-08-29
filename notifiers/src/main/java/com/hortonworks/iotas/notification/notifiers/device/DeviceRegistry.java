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
    private List<DeviceOperation> deviceOperationList;
    private List<DeviceAttribute> deviceAttributeList;

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

    public List<DeviceOperation> getDeviceOperationList() {
        return deviceOperationList;
    }

    public void setDeviceOperationList(List<DeviceOperation> deviceOperationList) {
        this.deviceOperationList = deviceOperationList;
    }

    public List<DeviceAttribute> getDeviceAttributeList() {
        return deviceAttributeList;
    }

    public void setDeviceAttributeList(List<DeviceAttribute> deviceAttributeList) {
        this.deviceAttributeList = deviceAttributeList;
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
        if (deviceOperationList != null ? !deviceOperationList.equals(that.deviceOperationList) : that.deviceOperationList != null)
            return false;
        return deviceAttributeList != null ? deviceAttributeList.equals(that.deviceAttributeList) : that.deviceAttributeList == null;

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
        result = 31 * result + (deviceOperationList != null ? deviceOperationList.hashCode() : 0);
        result = 31 * result + (deviceAttributeList != null ? deviceAttributeList.hashCode() : 0);
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
                ", deviceOperationsList=" + deviceOperationList +
                ", deviceAttributesList=" + deviceAttributeList +
                '}';
    }
}
