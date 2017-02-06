package com.hortonworks.streamline.streams.catalog.exception;

public class ServiceNotFoundException extends EntityNotFoundException {
    public ServiceNotFoundException(String message) {
        super(message);
    }

    public ServiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceNotFoundException(Throwable cause) {
        super(cause);
    }

    public ServiceNotFoundException(Long clusterId, String serviceName) {
        this("Service [" + serviceName + "] not found in cluster with id [" + clusterId + "]");
    }
}
