package com.hortonworks.iotas.notification.notifiers.device;

import com.hortonworks.iotas.notification.notifiers.device.protocol.Protocol;

import java.util.List;

/**
 * This class represents how a device would be represented in the device registry.
 */
public class DeviceInstance {
    /**
     * This field uniquely identifies a device.
     */
    private DeviceInfo deviceInfo;

    /**
     * This field has all the names of the protocol and the properties related to it.
     */
    private Protocol protocol;

    /**
     * A list of operations that can be performed on the device.
     */
    private List<DeviceOperation> operations;

    /**
     * A list of attributes that the device has.
     */
    private List<DeviceAttribute> attributes;

    //for jackson
    public DeviceInstance() {
    }

    public DeviceInstance(DeviceInfo deviceInfo, Protocol protocol, List<DeviceOperation> operations, List<DeviceAttribute> attributes) {
        this.deviceInfo = deviceInfo;
        this.protocol = protocol;
        this.operations = operations;
        this.attributes = attributes;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public List<DeviceOperation> getOperations() {
        return operations;
    }

    public void setOperations(List<DeviceOperation> operations) {
        this.operations = operations;
    }

    public List<DeviceAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<DeviceAttribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceInstance)) return false;

        DeviceInstance that = (DeviceInstance) o;

        if (deviceInfo != null ? !deviceInfo.equals(that.deviceInfo) : that.deviceInfo != null) return false;
        if (protocol != null ? !protocol.equals(that.protocol) : that.protocol != null) return false;
        if (operations != null ? !operations.equals(that.operations) : that.operations != null) return false;
        return attributes != null ? attributes.equals(that.attributes) : that.attributes == null;

    }

    @Override
    public int hashCode() {
        int result = deviceInfo != null ? deviceInfo.hashCode() : 0;
        result = 31 * result + (protocol != null ? protocol.hashCode() : 0);
        result = 31 * result + (operations != null ? operations.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DeviceInstance{" +
                "deviceInfo=" + deviceInfo +
                ", protocol=" + protocol +
                ", operations=" + operations +
                ", attributes=" + attributes +
                '}';
    }
}
