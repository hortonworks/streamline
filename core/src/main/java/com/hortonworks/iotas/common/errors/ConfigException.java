package com.hortonworks.iotas.common.errors;

import com.hortonworks.iotas.processor.CustomProcessor;

/**
 * Exception class representing a malformed configuration for a {@link CustomProcessor} implementation
 */
public class ConfigException extends Exception {

    public ConfigException (String message) { super(message); }

    public ConfigException (Throwable cause) { super(cause); }

    public ConfigException (String message, Throwable cause) { super(message, cause); }
}
