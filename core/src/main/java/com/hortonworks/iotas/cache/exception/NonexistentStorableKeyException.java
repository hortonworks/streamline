package com.hortonworks.iotas.cache.exception;

/**
 * Exception thrown if no value exists for a specific {@link com.hortonworks.iotas.storage.StorableKey} key,
 * i.e. no key exists in storage.
 * */
public class NonexistentStorableKeyException extends RuntimeException {
    public NonexistentStorableKeyException(String message) {
        super(message);
    }
}
