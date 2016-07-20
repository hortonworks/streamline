package com.hortonworks.iotas.cache.exception;

//TODO: Should this the a Checked Exception instead of a RuntimeException
public class CacheException extends RuntimeException {

    public CacheException(Throwable cause) {
        super(cause);
    }

    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }

    public CacheException(String message) {
        super(message);
    }
}
