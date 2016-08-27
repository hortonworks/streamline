package com.hortonworks.iotas.notification.notifiers.device;

import java.util.LinkedHashMap;
import java.util.Map;

public class MQTTMessageFactoryTypeOne implements MQTTMessageFactory{

    private static final String PAYLOAD_DEVICE_ID = "deviceId";
    private static final String PAYLOAD_DEVICE_NAME = "deviceName";
    private static final String PAYLOAD_DEVICE_MAKE = "deviceMake";
    private static final String PAYLOAD_DEVICE_MODEL = "deviceModel";
    private static final String PAYLOAD_OPERATION = "operation";

    private Map<String, Object> payloadMap;
    private Map<String, Object> objectMap;

    //for jackson
    public MQTTMessageFactoryTypeOne() {
    }

    public MQTTMessageFactoryTypeOne(Map<String, Object> objectMap) {
        this.objectMap = objectMap;
    }

    @Override
    public Map<String, Object> createPayloadMap(Map <String, Object> fieldsFromDeviceRegistry) {
        payloadMap = new LinkedHashMap<>();
        payloadMap.put(PAYLOAD_DEVICE_ID,fieldsFromDeviceRegistry.get("id"));
        payloadMap.put(PAYLOAD_DEVICE_NAME,fieldsFromDeviceRegistry.get("name"));
        payloadMap.put(PAYLOAD_DEVICE_MAKE,fieldsFromDeviceRegistry.get("model"));
        payloadMap.put(PAYLOAD_DEVICE_MODEL,fieldsFromDeviceRegistry.get("make"));
        payloadMap.put(PAYLOAD_OPERATION,objectMap);
        return payloadMap;
    }
}
