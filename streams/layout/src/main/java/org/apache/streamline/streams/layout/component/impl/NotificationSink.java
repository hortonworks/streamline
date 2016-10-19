package org.apache.streamline.streams.layout.component.impl;

import org.apache.streamline.streams.layout.component.IotasSink;
import org.apache.streamline.streams.layout.component.TopologyDagVisitor;

import java.util.Map;

public class NotificationSink extends IotasSink {
    private static final String CONFIG_KEY_NAME = "notifierName";
    private static final String CONFIG_KEY_JAR_FILENAME = "jarFileName";
    private static final String CONFIG_KEY_CLASSNAME = "className";
    private static final String CONFIG_KEY_PROPERTIES = "properties";
    private static final String CONFIG_KEY_FIELD_VALUES = "fieldValues";

    public String getNotifierName() {
        return getConfig().get(CONFIG_KEY_NAME);
    }

    public String getNotifierJarFileName() {
        return getConfig().get(CONFIG_KEY_JAR_FILENAME);
    }

    public String getNotifierClassName() {
        return getConfig().get(CONFIG_KEY_CLASSNAME);
    }

    public Map<String, Object> getNotifierProperties() {
        return getConfig().getAny(CONFIG_KEY_PROPERTIES);
    }

    public Map<String, Object> getNotifierFieldValues() {
        return getConfig().getAny(CONFIG_KEY_FIELD_VALUES);
    }

    @Override
    public void accept(TopologyDagVisitor visitor) {
        visitor.visit(this);
    }
}
