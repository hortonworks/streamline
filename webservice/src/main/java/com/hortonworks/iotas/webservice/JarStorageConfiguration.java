package com.hortonworks.iotas.webservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Map;

/**
 * The configuration for jar storage.
 *
 */
public class JarStorageConfiguration {

    @NotEmpty
    private String className;

    private Map<String, String> properties;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> config) {
        this.properties = config;
    }
}
