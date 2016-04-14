package com.hortonworks.iotas.layout.schema;

/**
 * An Exception class which represents that there's some issue of component configuration.
 */
public class BadComponentConfigException extends Exception {

    public BadComponentConfigException(String message) {
        super(message);
    }

    public BadComponentConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadComponentConfigException(Throwable cause) {
        super(cause);
    }

}
