package com.hortonworks.iotas.notification.common;


/**
 * A context object used by the notification service
 * to initialize the notifier. This contains the notifier config
 * and the ack and fail actions.
 */
public interface NotificationContext {
    /**
     * <p>
     * Returns the {@link NotifierConfig}, which contains the notifier properties and
     * default values for some of the fields.
     * E.g. email-id, to, from, subject etc for an email notifier.
     * </p>
     *
     * @return the notifier config.
     */
    NotifierConfig getConfig();

    /**
     * <p>
     * A notifier can invoke the ack method to indicate that a notification has been
     * successfully delivered to the external system (e.g. sms successfully delivered).
     * </p>
     *
     * @param notificationId the notification id.
     */
    void ack(String notificationId);

    /**
     * <p>
     * A notifier can invoke the fail method to indicate that there was some
     * issue in delivering the notification to the external system (e.g. email send failed).
     * Based on the configuration, the framework can re-deliver the notification based on a
     * retry count.
     * </p>
     *
     * @param notificationId the id of the notification that is failed.
     */
    void fail(String notificationId);
}
