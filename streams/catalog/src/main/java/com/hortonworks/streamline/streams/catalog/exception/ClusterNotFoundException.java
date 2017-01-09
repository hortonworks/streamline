package com.hortonworks.streamline.streams.catalog.exception;


public class ClusterNotFoundException extends EntityNotFoundException {
    public ClusterNotFoundException(String message) {
        super(message);
    }

    public ClusterNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClusterNotFoundException(Throwable cause) {
        super(cause);
    }

    public ClusterNotFoundException(Long clusterId) {
        this("Cluster [" + clusterId + "] not found");
    }
}
