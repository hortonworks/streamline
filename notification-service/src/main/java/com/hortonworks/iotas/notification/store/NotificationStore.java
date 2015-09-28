package com.hortonworks.iotas.notification.store;

import com.hortonworks.iotas.notification.common.Notification;

import java.util.List;

/**
 * For storing and retrieving the notifications
 * based on different criteria.
 *
 * Right now the only update operations on the notifications is to
 * set the delivered or failed status.
 */
public interface NotificationStore {
    /**
     * Store a notification in the notification store
     * @param notification the notification object
     */
    void store(Notification notification);

    /**
     * Update the notification status to delivered.
     *
     * @param notificationId the notification id
     */
    void setDelivered(String notificationId);

    /**
     * Update the notification status to failed.
     *
     * @param notificationId the notification id
     */
    void setFailed(String notificationId);

    /**
     * Look up a notification object based on notification id.
     *
     * @param notificationId the notification id
     * @return the notification
     */
    Notification get(String notificationId);

    /**
     * Returns the notifications from the store based on some criteria.
     *
     * @return the notifications
     */
    List<Notification> find(Criteria criteria);

    /**
     * Close connections with the data store and clean up.
     */
    void close();
}
