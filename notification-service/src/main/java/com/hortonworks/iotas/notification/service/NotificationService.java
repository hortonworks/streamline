package com.hortonworks.iotas.notification.service;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationContext;
import com.hortonworks.iotas.notification.common.Notifier;

import java.util.List;

/**
 * <p>
 * This interface represents the basic services provided for IoTaS push
 * and pull notifications.
 * <p/>
 * This could be plugged into any stream processing framework.
 * </p>
 */
public interface NotificationService {

    /**
     * Register a notifier object in the system.
     *
     * @param notifierName the user provided notifierName by which we will refer this instance.
     * @param ctx the notification context to initialize the notifier with
     * @return the registered notifier
     */
    Notifier register(String notifierName, NotificationContext ctx);

    /**
     * De-registers a notifier if no longer needed
     *
     * @param notifierName the unique id of the notifier.
     *
     * @return the removed notifier or null if there was no such notifier.
     */
    Notifier remove(String notifierName);

    /**
     * Sends notification to a notifier.
     *
     * @param notifierName the notifier name
     * @param notification the notification object.
     */
    void notify(String notifierName, Notification notification);

    /**
     * <p>
     *     Gets notifications from the store. Pull notifiers can use this to fetch
     *     notifications asynchronously.
     * </p>
     * TODO: add options to fetch based on time range, status etc
     * TODO: and options to limit the count.
     * @return the list of unprocessed notifications stored for the default pull notifier.
     */
    List<Notification> getNotifications();

    //TODO: add methods to retrieve notifications based on query criteria
}
