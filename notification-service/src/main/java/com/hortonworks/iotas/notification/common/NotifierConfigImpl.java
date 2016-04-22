package com.hortonworks.iotas.notification.common;

import com.hortonworks.iotas.notification.NotifierConfig;

import java.util.Map;
import java.util.Properties;

/**
 * The notifier config implementation.
 */
public class NotifierConfigImpl implements NotifierConfig {

    private final Properties properties;
    private final Map<String, String> defaultFieldValues;
    private final String className;
    private final String jarPath;

    public NotifierConfigImpl(Properties notifierProps, Map<String, String> defaultFieldValues,
                              String className, String jarPath) {
        this.properties = notifierProps;
        this.defaultFieldValues = defaultFieldValues;
        this.className = className;
        this.jarPath = jarPath;
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
    public String getClassName() {
        return className;
    }

    @Override
    public String getJarPath() {
        return jarPath;
    }

    @Override
    public String toString() {
        return "NotifierConfigImpl{" +
                "properties=" + properties +
                ", defaultFieldValues=" + defaultFieldValues +
                ", className='" + className + '\'' +
                ", jarPath='" + jarPath + '\'' +
                '}';
    }
}
