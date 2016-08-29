package com.hortonworks.iotas.notification.notifiers.device;

import java.util.LinkedHashMap;
import java.util.Map;

public class MQTTMessagePayloadFactoryTypeOne implements MQTTMessagePayloadFactory {

    private static final String REGISTRY_DEVICE_ID = "deviceIdentificationField";
    private static final String PAYLOAD_DEVICE_ID = "id";
    private static final String PAYLOAD_DEVICE_NAME = "name";
    private static final String PAYLOAD_DEVICE_MAKE = "make";
    private static final String PAYLOAD_DEVICE_MODEL = "model";
    private static final String PAYLOAD_OPERATION = "operation";

    private Map<String, Object> payloadMap;
    private Map<String, Object> objectMap;

    //for jackson
    public MQTTMessagePayloadFactoryTypeOne() {
    }

    public MQTTMessagePayloadFactoryTypeOne(Map<String, Object> objectMap) {
        this.objectMap = objectMap;
    }

    @Override
    public Map<String, Object> createPayloadMap(Map <String, Object> fieldsFromDeviceRegistry) {
        payloadMap = new LinkedHashMap<>();
        Map<String,Object> deviceIdentificationField = (Map<String,Object>)fieldsFromDeviceRegistry.get(REGISTRY_DEVICE_ID);
        payloadMap.put(PAYLOAD_DEVICE_ID,deviceIdentificationField.get(PAYLOAD_DEVICE_ID));
        payloadMap.put(PAYLOAD_DEVICE_NAME,deviceIdentificationField.get(PAYLOAD_DEVICE_NAME));
        payloadMap.put(PAYLOAD_DEVICE_MAKE,deviceIdentificationField.get(PAYLOAD_DEVICE_MAKE));
        payloadMap.put(PAYLOAD_DEVICE_MODEL,deviceIdentificationField.get(PAYLOAD_DEVICE_MODEL));
        payloadMap.put(PAYLOAD_OPERATION,objectMap);
        return payloadMap;
    }
}
