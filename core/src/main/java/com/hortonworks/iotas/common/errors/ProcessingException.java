package com.hortonworks.iotas.common.errors;

import com.hortonworks.iotas.processor.CustomProcessor;

/**
 * Exception class representing an error while processing a message by a {@link CustomProcessor} implementation
 */
public class ProcessingException extends Exception {

    public ProcessingException (String message) { super(message); }

    public ProcessingException (Throwable cause) { super(cause); }

    public ProcessingException (String message, Throwable cause) { super(message, cause); }
}
