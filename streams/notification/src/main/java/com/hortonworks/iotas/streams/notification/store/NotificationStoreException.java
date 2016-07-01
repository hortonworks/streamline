package com.hortonworks.iotas.streams.notification.store;

/**
 * Indicates a problem while trying to store data in notification store.
 */
public class NotificationStoreException extends RuntimeException {

    public NotificationStoreException(String msg) {
        super(msg);
    }

    public NotificationStoreException(String msg, Throwable t) {
        super(msg, t);
    }
}
