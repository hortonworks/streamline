package com.hortonworks.iotas.storage.exception;

//TODO: Should this the a Checked Exception instead of a RuntimeException
public class StorageException extends RuntimeException {

    public StorageException(Throwable cause) {
        super(cause);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageException(String message) {
        super(message);
    }
}
