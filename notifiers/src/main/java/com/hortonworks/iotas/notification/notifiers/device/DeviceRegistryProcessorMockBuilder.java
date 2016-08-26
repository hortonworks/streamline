package com.hortonworks.iotas.notification.notifiers.device;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.component.ComponentBuilder;

import java.util.ArrayList;
import java.util.List;

public class DeviceRegistryProcessorMockBuilder  implements ComponentBuilder<DeviceRegistryProcessor> {

    public static final String DEVICE_REGISTRY_PROCESSOR = "device_registry_processor";
    public static final String MAKE = "make";
    public static final String MODEL = "model";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String BROKER = "tcp://10.22.9.209:1883";
    public static final String TOPIC = "topic";
    public static final String OPERATION = "operation";
    public static final String ATTRIBUTE = "attribute";
    public static final String PARAM = "paramName";

    private final long deviceRegistryProcessorId;
    int numberOfDevices;
    int numberOfAttributesPerDevice;
    int numberOfOperationsPerDevice;

    public DeviceRegistryProcessorMockBuilder(long deviceRegistryProcessorId, int numberOfDevices, int numberOfAttributesPerDevice, int numberOfOperationsPerDevice) {
        this.deviceRegistryProcessorId = deviceRegistryProcessorId;
        this.numberOfDevices = numberOfDevices;
        this.numberOfAttributesPerDevice = numberOfAttributesPerDevice;
        this.numberOfOperationsPerDevice = numberOfOperationsPerDevice;
    }

    @Override
    public DeviceRegistryProcessor build() {
        DeviceRegistryProcessor deviceRegistryProcessor = new DeviceRegistryProcessor();
        deviceRegistryProcessor.setId(String.valueOf(deviceRegistryProcessorId));
        deviceRegistryProcessor.setName(DEVICE_REGISTRY_PROCESSOR + "_" + deviceRegistryProcessorId);
        deviceRegistryProcessor.setDevices(buildDevices());
        return deviceRegistryProcessor;
    }

    private List<DeviceRegistry> buildDevices() {
        List<DeviceRegistry> devices = new ArrayList<>();
        for(int i = 1; i <= numberOfDevices; i++){
            devices.add(buildDevice((long)i));
        }
        return devices;
    }

    private DeviceRegistry buildDevice(long deviceId) {
        DeviceRegistry deviceRegistry = new DeviceRegistry();
        deviceRegistry.setId(deviceId);
        deviceRegistry.setName(NAME + deviceId);
        deviceRegistry.setDescription(DESCRIPTION + deviceId);
        deviceRegistry.setMake(MAKE + deviceId);
        deviceRegistry.setModel(MODEL + deviceId);
        deviceRegistry.setProtocol(DeviceRegistry.Protocol.MQTT);
        deviceRegistry.setMqttBroker(BROKER);
        deviceRegistry.setTopic(TOPIC);
        deviceRegistry.setDeviceAttributesList(buildDeviceAttributesList());
        deviceRegistry.setDeviceOperationsList(buildDeviceOperationsList());
        return deviceRegistry;
    }

    private List<DeviceOperations> buildDeviceOperationsList() {
        List<DeviceOperations> deviceOperations = new ArrayList<>();
        for (int i = 1; i <= numberOfOperationsPerDevice; i++) {
            deviceOperations.add(buildDeviceOperation(i));
        }
        return deviceOperations;
    }

    private DeviceOperations buildDeviceOperation(int operationId) {
        DeviceOperations deviceOperations = new DeviceOperations();
        deviceOperations.setName(OPERATION + operationId);
        deviceOperations.setNumParameters(1);
        List<Schema.Field> params = new ArrayList<>();
        params.add(new Schema.Field(PARAM,Schema.Type.INTEGER));
        deviceOperations.setParams(params);
        return deviceOperations;
    }

    private List<DeviceAttributes> buildDeviceAttributesList() {
        List<DeviceAttributes> deviceAttributes = new ArrayList<>();
        for (int i = 1; i <= numberOfAttributesPerDevice; i++) {
            deviceAttributes.add(buildDeviceAttribute(i));
        }
        return deviceAttributes;
    }

    private DeviceAttributes buildDeviceAttribute(int attributeId) {
        DeviceAttributes deviceAttributes = new DeviceAttributes();
        Schema.Field field = new Schema.Field(ATTRIBUTE + attributeId,Schema.Type.INTEGER);
        deviceAttributes.setField(field);
        return deviceAttributes;
    }
}