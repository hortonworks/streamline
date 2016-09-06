package com.hortonworks.iotas.notification.notifiers.device;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.notification.notifiers.device.protocol.Protocol;
import com.hortonworks.iotas.notification.notifiers.device.protocol.mqtt.MQTTProtocol;

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

    DeviceInstance deviceInstance = new DeviceInstance();
    int numOfAttributes;
    int numOfOperations;

    public MQTTDeviceIdentityMockBuilder() {
    }

    public MQTTDeviceIdentityMockBuilder(int numOfAttributes, int numOfOperations) {
        this.numOfAttributes = numOfAttributes;
        this.numOfOperations = numOfOperations;
    }

    public DeviceInstance getDeviceInstance() {
        return deviceInstance;
    }

    public void build() {
        deviceInstance.setDeviceInfo(buildDeviceIdentifiers());
        deviceInstance.setProtocol(buildProtocolProperties());
        deviceInstance.setAttributes(buildDeviceAttributes());
        deviceInstance.setOperations(buildDeviceOperations());
    }

    private DeviceInfo buildDeviceIdentifiers() {
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setId(UUID.randomUUID().toString());
        deviceInfo.setMake(MAKE);
        deviceInfo.setModel(MODEL);
        deviceInfo.setName(NAME);
        deviceInfo.setDescription(DESCRIPTION);
        return deviceInfo;
    }

    private Protocol buildProtocolProperties() {
        Protocol protocol = new MQTTProtocol(TOPIC, BROKER);
        return protocol;
    }

    private List<DeviceAttribute> buildDeviceAttributes() {
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

    private List<DeviceOperation> buildDeviceOperations() {
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
        deviceOperation.setParams(buildDeviceOperationParameters(numParameters));
        return deviceOperation;
    }

    private List<Schema.Field> buildDeviceOperationParameters(int numParameters) {
        List<Schema.Field> deviceOperationParamsList = new ArrayList<>();
        for (int i = 1; i <= numParameters; i++) {
            deviceOperationParamsList.add(new Schema.Field(PARAM + i, Schema.Type.INTEGER));
        }
        return deviceOperationParamsList;
    }
}
