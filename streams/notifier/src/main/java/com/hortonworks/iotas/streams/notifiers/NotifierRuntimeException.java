package com.hortonworks.iotas.streams.notifiers;

/**
 * Signals some runtime issue with a notifier
 */
public class NotifierRuntimeException extends RuntimeException {
    public NotifierRuntimeException(String msg) {
        super(msg);
    }
    public NotifierRuntimeException(String msg, Throwable th) {
        super(msg, th);
    }
    public NotifierRuntimeException(Throwable th) {
        super(th);
    }
}
