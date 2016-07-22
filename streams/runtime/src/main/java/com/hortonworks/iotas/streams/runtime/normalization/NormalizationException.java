package com.hortonworks.iotas.streams.runtime.normalization;

/**
 * It is thrown when an issue occurs while normalizing the event.
 * For ex: When an event is transformed by using a script and script throws a ScriptException
 */
public class NormalizationException extends Exception {
    public NormalizationException() {
    }

    public NormalizationException(String message) {
        super(message);
    }

    public NormalizationException(String message, Throwable cause) {
        super(message, cause);
    }

    public NormalizationException(Throwable cause) {
        super(cause);
    }

    public NormalizationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
