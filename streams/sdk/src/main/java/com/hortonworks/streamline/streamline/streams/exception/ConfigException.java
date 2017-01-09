package com.hortonworks.streamline.streams.exception;

/**
 * Exception class representing a malformed configuration.
 */
public class ConfigException extends Exception {

    public ConfigException (String message) { super(message); }

    public ConfigException (Throwable cause) { super(cause); }

    public ConfigException (String message, Throwable cause) { super(message, cause); }
}
