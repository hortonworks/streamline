package com.hortonworks.iotas.parser;

/**
 * Indicates an issue while trying to parse the data.
 * Parser implementations can wrap the low level exceptions in this class.
 */
public class ParseException extends Exception {

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParseException(Throwable cause) {
        super(cause);
    }
}
