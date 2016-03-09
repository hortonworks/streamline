package com.hortonworks.iotas.exception;

/**
 * Base class of exceptions that should be thrown when an exceptional situation occurs while parsing some input of data.
 * More specific exceptions can extend this exception to indicate more fine grade situations which the parser client logic
 * (e.g. {@code ParserBolt}) is expected to handle.
 * Parser implementations can wrap the low level exceptions in this class.
 */
public class ParserException extends Exception {

    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParserException(Throwable cause) {
        super(cause);
    }
}
