package com.hortonworks.iotas.notification.common;

import com.hortonworks.iotas.notification.NotificationContext;
import com.hortonworks.iotas.notification.NotifierConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A default notification context that can be overridden to provide custom
 * ack and fail behavior.
 */
public class DefaultNotificationContext implements NotificationContext {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultNotificationContext.class);

    private final NotifierConfig notifierConfig;

    public DefaultNotificationContext(NotifierConfig notifierConfig) {
        this.notifierConfig = notifierConfig;
    }

    @Override
    public NotifierConfig getConfig() {
        return notifierConfig;
    }

    @Override
    public void ack(String notificationId) {
        LOG.debug("DefaultNotificationContext ack, no-op");
    }

    @Override
    public void fail(String notificationId) {
        LOG.debug("DefaultNotificationContext fail, no-op");
    }

    @Override
    public String toString() {
        return "DefaultNotificationContext{" +
                "notifierConfig=" + notifierConfig +
                '}';
    }
}
