package com.hortonworks.iotas.notification.notifiers.device;

import java.util.HashMap;
import java.util.Map;

public class DeviceRegistryMock {
    Map<String,Object> deviceRegistry = new HashMap<>();

    public DeviceRegistryMock(Map<String, Object> deviceRegistry) {
        this.deviceRegistry = deviceRegistry;
    }

    public Map<String, Object> getDeviceRegistry() {
        return deviceRegistry;
    }

    public void setDeviceRegistry(Map<String, Object> deviceRegistry) {
        this.deviceRegistry = deviceRegistry;
    }

    public void addDeviceToRegistry(String key, Object value) {
        deviceRegistry.put(key,value);
    }

    public void removeDeviceFromRegistry(String key) {
        deviceRegistry.remove(key);
    }

    public void test() {
        MQTTDeviceIdentityMockBuilder mqttDeviceIdentityMockBuilder = new MQTTDeviceIdentityMockBuilder(2,2);
    }
}
