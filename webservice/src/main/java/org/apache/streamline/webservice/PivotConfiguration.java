package org.apache.streamline.webservice;



import javax.validation.constraints.NotNull;

import java.util.Map;

public class PivotConfiguration {

    @NotNull
    private Integer port;

    private Map<String, Object> config;

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

}
