package org.apache.streamline.streams.catalog.service.metadata.common;

public class HostPort {
    private final String host;
    private final Integer port;

    public HostPort(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
