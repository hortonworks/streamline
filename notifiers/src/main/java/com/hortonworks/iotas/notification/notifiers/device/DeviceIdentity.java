package com.hortonworks.iotas.notification.notifiers.device;

import java.util.List;

public class DeviceIdentity {
    public enum Protocol {
        MQTT, COAP, AMPQ
    }

    private DeviceIdentificationField deviceIdentificationField;
    private Protocol protocolName;
    private ProtocolField protocolField;
    private List<DeviceOperation> deviceOperationList;
    private List<DeviceAttribute> deviceAttributeList;

    //for jackson
    public DeviceIdentity() {
    }

    public DeviceIdentity(DeviceIdentificationField deviceIdentificationField, Protocol protocolName, ProtocolField protocolField, List<DeviceOperation> deviceOperationList, List<DeviceAttribute> deviceAttributeList) {
        this.deviceIdentificationField = deviceIdentificationField;
        this.protocolName = protocolName;
        this.protocolField = protocolField;
        this.deviceOperationList = deviceOperationList;
        this.deviceAttributeList = deviceAttributeList;
    }

    public DeviceIdentificationField getDeviceIdentificationField() {
        return deviceIdentificationField;
    }

    public void setDeviceIdentificationField(DeviceIdentificationField deviceIdentificationField) {
        this.deviceIdentificationField = deviceIdentificationField;
    }

    public Protocol getProtocolName() {
        return protocolName;
    }

    public void setProtocolName(Protocol protocolName) {
        this.protocolName = protocolName;
    }

    public ProtocolField getProtocolField() {
        return protocolField;
    }

    public void setProtocolField(ProtocolField protocolField) {
        this.protocolField = protocolField;
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
        if (!(o instanceof DeviceIdentity)) return false;

        DeviceIdentity that = (DeviceIdentity) o;

        if (deviceIdentificationField != null ? !deviceIdentificationField.equals(that.deviceIdentificationField) : that.deviceIdentificationField != null)
            return false;
        if (protocolName != that.protocolName) return false;
        if (protocolField != null ? !protocolField.equals(that.protocolField) : that.protocolField != null)
            return false;
        if (deviceOperationList != null ? !deviceOperationList.equals(that.deviceOperationList) : that.deviceOperationList != null)
            return false;
        return deviceAttributeList != null ? deviceAttributeList.equals(that.deviceAttributeList) : that.deviceAttributeList == null;

    }

    @Override
    public int hashCode() {
        int result = deviceIdentificationField != null ? deviceIdentificationField.hashCode() : 0;
        result = 31 * result + (protocolName != null ? protocolName.hashCode() : 0);
        result = 31 * result + (protocolField != null ? protocolField.hashCode() : 0);
        result = 31 * result + (deviceOperationList != null ? deviceOperationList.hashCode() : 0);
        result = 31 * result + (deviceAttributeList != null ? deviceAttributeList.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DeviceIdentity{" +
                "deviceIdentificationField=" + deviceIdentificationField +
                ", protocolName=" + protocolName +
                ", protocolField=" + protocolField +
                ", deviceOperationsList=" + deviceOperationList +
                ", deviceAttributesList=" + deviceAttributeList +
                '}';
    }
}
