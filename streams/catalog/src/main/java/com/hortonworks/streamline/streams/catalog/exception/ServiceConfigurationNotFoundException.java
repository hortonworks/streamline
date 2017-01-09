package org.apache.streamline.streams.catalog.exception;


import org.apache.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;

public class ServiceConfigurationNotFoundException extends EntityNotFoundException {
    public ServiceConfigurationNotFoundException(String message) {
        super(message);
    }

    public ServiceConfigurationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceConfigurationNotFoundException(Throwable cause) {
        super(cause);
    }

    public ServiceConfigurationNotFoundException(Long clusterId, ServiceConfigurations service, String configurationName) {
        this(String.format("Configuration [%s] not found for service [%s] in cluster with id [%d]", configurationName, service.name(), clusterId));
    }
}
