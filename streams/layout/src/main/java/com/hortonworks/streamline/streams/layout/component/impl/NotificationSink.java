package com.hortonworks.streamline.streams.layout.component.impl;

import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class NotificationSink extends StreamlineSink {
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
    public Set<String> getExtraJars() {
        return Collections.singleton(getNotifierJarFileName());
    }

    @Override
    public void accept(TopologyDagVisitor visitor) {
        visitor.visit(this);
    }
}
