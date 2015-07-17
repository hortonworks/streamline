package com.hortonworks.iotas.storage;

public class AlreadyExistsException extends StorageException {

    public AlreadyExistsException(String message) {
        super(message);
    }
}
