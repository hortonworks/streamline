package com.hortonworks.iotas.notification;

import java.util.List;

/**
 * <p>
 * A notifier would implement this interface. The notifier
 * would be notified by the framework whenever there is an event
 * (based on defined rules).
 * </p>
 */
public interface Notifier {

    /**
     * <p>
     * Invoked by the framework when a notifier is initialized.
     * The {@link NotificationContext} can be used for getting
     * the config and also for sending ack/fail status of a notification.
     * </p>
     *
     * @param ctx the notification context.
     */
    void open(NotificationContext ctx);


    /**
     * <p>
     * The framework would invoke this method on a notifier. The notifier
     * takes the {@link Notification} object, constructs a message and delivers
     * it to the external system (e.g. send an email).
     * </p>
     *
     * @param notification the Notification object
     */
    void notify(Notification notification);

    /**
     * <p>
     * Invoked when a notifier is de-registered from the framework.
     * </p>
     */
    void close();

    /**
     * <p>
     * Returns if this notifier is a pull notifier or not.
     * <p/>
     * Pull notifiers would fetch notifications from the framework asynchronously.
     * </p>
     *
     * @return if this notifier is pull or not.
     */
    boolean isPull();

    /**
     * The fields that this notifier processes.
     * E.g[email-id, from, to, subject, body] for EmailNotifier.
     * This can be used by the framework to create the
     * Notification object. The default values for the fields could
     * also be specified in the config.
     */
    List<String> getFields();

    /**
     * Returns the notification context that this notifier is initialized with.
     *
     */
    NotificationContext getContext();
}
