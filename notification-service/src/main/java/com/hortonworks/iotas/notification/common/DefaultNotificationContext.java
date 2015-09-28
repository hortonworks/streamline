package com.hortonworks.iotas.notification.common;

/**
 * A default notification context that can be overridden to provide custom
 * ack and fail behavior.
 */
public class DefaultNotificationContext implements NotificationContext {
    private final NotifierConfig config;

    public DefaultNotificationContext(NotifierConfig notifierConfig) {
        this.config = notifierConfig;
    }
    @Override
    public NotifierConfig getConfig() {
        return config;
    }

    @Override
    public void ack(String notificationId) {
        // NO OP
    }

    @Override
    public void fail(String notificationId) {
        // NO OP
    }

    @Override
    public String toString() {
        return "DefaultNotificationContext{" +
                "config=" + config +
                '}';
    }
}
