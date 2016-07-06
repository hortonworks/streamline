package com.hortonworks.iotas.common.exception;

/**
 * Exception class representing an error while processing a message
 */
public class ProcessingException extends Exception {

    public ProcessingException (String message) { super(message); }

    public ProcessingException (Throwable cause) { super(cause); }

    public ProcessingException (String message, Throwable cause) { super(message, cause); }
}
