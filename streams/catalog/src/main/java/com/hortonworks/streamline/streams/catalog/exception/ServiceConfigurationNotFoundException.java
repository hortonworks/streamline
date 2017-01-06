package com.hortonworks.streamline.streams.catalog.exception;


import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;

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
