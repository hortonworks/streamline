package com.hortonworks.streamline.streams.layout.exception;

/**
 * Indicates an issue while trying to validate a json for a topology layout.
 */
public class ComponentConfigException extends Exception {

    public ComponentConfigException(String message) {
        super(message);
    }

    public ComponentConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public ComponentConfigException(Throwable cause) {
        super(cause);
    }
}
