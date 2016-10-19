package org.apache.streamline.storage.tool.conf;

import java.util.Map;

/**
 * This class represents storage provider configuration.
 * This is from webservice module, and should be same as StorageProviderConfiguration in webservice.
 */
public class StorageProviderConfiguration {
    private String providerClass;

    private Map<String, Object> properties;

    public StorageProviderConfiguration() {
    }
    public String getProviderClass() {
        return providerClass;
    }

    public void setProviderClass(String providerClass) {
        this.providerClass = providerClass;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}