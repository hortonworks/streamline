package com.hortonworks.iotas.notification.notifiers.device;

import java.util.List;

public class DeviceIdentity {
    public enum Protocol {
        MQTT, COAP, AMPQ
    }

    private DeviceIdentificationField deviceIdentificationField;
    private Protocol protocolName;
    private ProtocolField protocolField;
    private List<DeviceOperations> deviceOperationsList;
    private List<DeviceAttributes> deviceAttributesList;

    //for jackson
    public DeviceIdentity() {
    }

    public DeviceIdentity(DeviceIdentificationField deviceIdentificationField, Protocol protocolName, ProtocolField protocolField, List<DeviceOperations> deviceOperationsList, List<DeviceAttributes> deviceAttributesList) {
        this.deviceIdentificationField = deviceIdentificationField;
        this.protocolName = protocolName;
        this.protocolField = protocolField;
        this.deviceOperationsList = deviceOperationsList;
        this.deviceAttributesList = deviceAttributesList;
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
        if (!(o instanceof DeviceIdentity)) return false;

        DeviceIdentity that = (DeviceIdentity) o;

        if (deviceIdentificationField != null ? !deviceIdentificationField.equals(that.deviceIdentificationField) : that.deviceIdentificationField != null)
            return false;
        if (protocolName != that.protocolName) return false;
        if (protocolField != null ? !protocolField.equals(that.protocolField) : that.protocolField != null)
            return false;
        if (deviceOperationsList != null ? !deviceOperationsList.equals(that.deviceOperationsList) : that.deviceOperationsList != null)
            return false;
        return deviceAttributesList != null ? deviceAttributesList.equals(that.deviceAttributesList) : that.deviceAttributesList == null;

    }

    @Override
    public int hashCode() {
        int result = deviceIdentificationField != null ? deviceIdentificationField.hashCode() : 0;
        result = 31 * result + (protocolName != null ? protocolName.hashCode() : 0);
        result = 31 * result + (protocolField != null ? protocolField.hashCode() : 0);
        result = 31 * result + (deviceOperationsList != null ? deviceOperationsList.hashCode() : 0);
        result = 31 * result + (deviceAttributesList != null ? deviceAttributesList.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DeviceIdentity{" +
                "deviceIdentificationField=" + deviceIdentificationField +
                ", protocolName=" + protocolName +
                ", protocolField=" + protocolField +
                ", deviceOperationsList=" + deviceOperationsList +
                ", deviceAttributesList=" + deviceAttributesList +
                '}';
    }
}
