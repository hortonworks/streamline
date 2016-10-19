package org.apache.streamline.streams.layout.exception;

/**
 * Indicates an issue while trying to validate a json for a topology layout.
 */
public class BadTopologyLayoutException extends Exception {

    public BadTopologyLayoutException(String message) {
        super(message);
    }

    public BadTopologyLayoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadTopologyLayoutException(Throwable cause) {
        super(cause);
    }
}
