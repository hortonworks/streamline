package com.hortonworks.iotas.layout.schema;

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
