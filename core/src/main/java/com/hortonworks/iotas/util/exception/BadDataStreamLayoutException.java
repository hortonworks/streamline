package com.hortonworks.iotas.util.exception;

/**
 * Indicates an issue while trying to validate a json for a data stream layout.
 */
public class BadDataStreamLayoutException extends Exception {

    public BadDataStreamLayoutException (String message) {
        super(message);
    }

    public BadDataStreamLayoutException (String message, Throwable cause) {
        super(message, cause);
    }

    public BadDataStreamLayoutException (Throwable cause) {
        super(cause);
    }
}
