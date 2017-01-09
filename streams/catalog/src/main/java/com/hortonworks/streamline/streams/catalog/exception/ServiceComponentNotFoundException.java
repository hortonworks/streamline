package com.hortonworks.streamline.streams.catalog.exception;


import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;

public class ServiceComponentNotFoundException extends EntityNotFoundException {
    public ServiceComponentNotFoundException(String message) {
        super(message);
    }

    public ServiceComponentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceComponentNotFoundException(Throwable cause) {
        super(cause);
    }

    public ServiceComponentNotFoundException(Long clusterId, ServiceConfigurations service, ComponentPropertyPattern component) {
        this(String.format("Component [%s] not found for service [%s] in cluster with id [%d]", component.name(), service.name(), clusterId));
    }
}
