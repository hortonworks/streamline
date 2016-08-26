package com.hortonworks.iotas.webservice;

import java.util.Map;

/**
 * A class representing information for a module to be registered with web service
 */
public class ModuleConfiguration {
    private String name;
    private String className;
    private Map<String, Object>  config;

    public String getClassName() {

        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
