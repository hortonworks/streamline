package com.hortonworks.streamline.webservice;



import javax.validation.constraints.NotNull;

import java.util.Map;

public class PivotConfiguration {

    @NotNull
    private Integer port;

    private Map<String, Object> config;

    private Map<String, Object> settingsLocation;

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public Map<String, Object> getSettingsLocation() { return settingsLocation; }

    public void setSettingsLocation(Map<String, Object> settingsLocation) { this.settingsLocation = settingsLocation; }
}
