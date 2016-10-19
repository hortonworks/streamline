package org.apache.streamline.streams.notification.service;

/**
 * Indicates a runtime issue in the notification service while processing
 * a request
 */
public class NotificationServiceException extends RuntimeException {
    public NotificationServiceException(String message) {
        super(message);
    }

    public NotificationServiceException(Throwable th) {
        super(th);
    }

    public NotificationServiceException(String message, Throwable th) {
        super(message, th);
    }
}
