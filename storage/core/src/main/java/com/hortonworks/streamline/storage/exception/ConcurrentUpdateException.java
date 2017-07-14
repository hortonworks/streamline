package com.hortonworks.streamline.storage.exception;

/**
 * Thrown when there are concurrent updates to the same record in the storage.
 * The clients can catch and attempt to retry the operation.
 */
public class ConcurrentUpdateException extends StorageException {

    public ConcurrentUpdateException(Throwable cause) {
        super(cause);
    }

    public ConcurrentUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConcurrentUpdateException(String message) {
        super(message);
    }
}
