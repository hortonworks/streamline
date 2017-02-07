package com.hortonworks.streamline.streams.catalog.exception;

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

    public ServiceComponentNotFoundException(Long clusterId, String serviceName, String componentName) {
        this(String.format("Component [%s] not found for service [%s] in cluster with id [%d]", componentName, serviceName, clusterId));
    }
}
