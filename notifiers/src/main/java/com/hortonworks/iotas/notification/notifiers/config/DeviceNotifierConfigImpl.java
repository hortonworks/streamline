package com.hortonworks.iotas.notification.notifiers.config;

import com.hortonworks.iotas.notification.notifiers.device.DeviceInstance;

import java.util.Map;
import java.util.Properties;

public class DeviceNotifierConfigImpl implements DeviceNotifierConfig {

    private final Properties properties;
    private final Map<String, String> defaultFieldValues;
    private final String className;
    private final String jarPath;
    private final DeviceInstance deviceInstance;

    public DeviceNotifierConfigImpl(Properties properties, Map<String, String> defaultFieldValues,
                                    String className, String jarPath, DeviceInstance deviceInstance) {
        this.properties = properties;
        this.defaultFieldValues = defaultFieldValues;
        this.className = className;
        this.jarPath = jarPath;
        this.deviceInstance = deviceInstance;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getJarPath() {
        return jarPath;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public Map<String, String> getDefaultFieldValues() {
        return defaultFieldValues;
    }

    @Override
    public DeviceInstance getDeviceMetaData() {
        return deviceInstance;
    }
}
