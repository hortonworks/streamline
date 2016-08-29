package com.hortonworks.iotas.notification.notifiers.device;

import com.hortonworks.iotas.common.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MQTTDeviceIdentityMockBuilder {
    public static final String MAKE = "make";
    public static final String MODEL = "model";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String BROKER = "tcp://iot.eclipse.org:1883";
    public static final String TOPIC = "topic";
    public static final String OPERATION = "operation";
    public static final String ATTRIBUTE = "attribute";
    public static final String PARAM = "param";

    DeviceIdentity deviceIdentity = new DeviceIdentity();
    int numOfAttributes;
    int numOfOperations;

    public MQTTDeviceIdentityMockBuilder() {
    }

    public MQTTDeviceIdentityMockBuilder(int numOfAttributes, int numOfOperations) {
        this.numOfAttributes = numOfAttributes;
        this.numOfOperations = numOfOperations;
    }

    public DeviceIdentity getDeviceIdentity() {
        return deviceIdentity;
    }

    public void build() {
        deviceIdentity.setDeviceIdentificationField(buildDeviceIdentificationField());
        deviceIdentity.setProtocolName(DeviceIdentity.Protocol.MQTT);
        deviceIdentity.setProtocolField(buildProtocolField());
        deviceIdentity.setDeviceAttributeList(buildDeviceAttributeList());
        deviceIdentity.setDeviceOperationList(buildDeviceOperationList());
    }

    private DeviceIdentificationField buildDeviceIdentificationField() {
        DeviceIdentificationField deviceIdentificationField = new DeviceIdentificationField();
        deviceIdentificationField.setId(UUID.randomUUID().toString());
        deviceIdentificationField.setMake(MAKE);
        deviceIdentificationField.setModel(MODEL);
        deviceIdentificationField.setName(NAME);
        deviceIdentificationField.setDescription(DESCRIPTION);
        return deviceIdentificationField;
    }

    private ProtocolField buildProtocolField() {
        MQTTProtocolField mqttProtocolField = new MQTTProtocolField();
        mqttProtocolField.setMqttTopic(TOPIC);
        mqttProtocolField.setMqttBrokerURL(BROKER);
        return mqttProtocolField;
    }

    private List<DeviceAttribute> buildDeviceAttributeList() {
        List<DeviceAttribute> deviceAttributeList = new ArrayList<>();
        for (int i = 1; i <= numOfAttributes; i++) {
            deviceAttributeList.add(buildDeviceAttribute(i));
        }
        return deviceAttributeList;
    }

    private DeviceAttribute buildDeviceAttribute(int attributeNumber) {
        DeviceAttribute deviceAttribute = new DeviceAttribute();
        deviceAttribute.setField(new Schema.Field(ATTRIBUTE + attributeNumber, Schema.Type.INTEGER));
        return deviceAttribute;
    }

    private List<DeviceOperation> buildDeviceOperationList() {
        List<DeviceOperation> deviceOperationList = new ArrayList<>();
        for (int i = 1; i <= numOfOperations; i++) {
            deviceOperationList.add(buildDeviceOperation(i));
        }
        return deviceOperationList;
    }

    private DeviceOperation buildDeviceOperation(int operationNumber) {
        DeviceOperation deviceOperation = new DeviceOperation();
        deviceOperation.setName(OPERATION + operationNumber);
        int numParameters = 1; //For ease of building, every device operation will have only one parameter
        deviceOperation.setNumParameters(numParameters);
        deviceOperation.setParams(buildDeviceOperationParamsList(numParameters));
        return deviceOperation;
    }

    private List<Schema.Field> buildDeviceOperationParamsList(int numParameters) {
        List<Schema.Field> deviceOperationParamsList = new ArrayList<>();
        for (int i = 1; i <= numParameters; i++) {
            deviceOperationParamsList.add(new Schema.Field(PARAM + i, Schema.Type.INTEGER));
        }
        return deviceOperationParamsList;
    }
}
