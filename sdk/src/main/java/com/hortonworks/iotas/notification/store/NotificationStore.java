package com.hortonworks.iotas.notification.store;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.notification.Notification;

import java.util.List;

/**
 * <p>
 * For storing and retrieving the notifications
 * based on different criteria.
 * Right now the only update operations on the notifications is to
 * set the delivered or failed status.
 * </p>
 */
public interface NotificationStore {
    /**
     * Store a notification in the notification store
     *
     * @param notification the notification object
     */
    void store(Notification notification);

    /**
     * Look up a notification object based on notification id.
     *
     * @param notificationId the notification id
     * @return the notification
     */
    Notification getNotification(String notificationId);

    /**
     * Look up notification objects from store based on notification id.
     *
     */
    List<Notification> getNotifications(List<String> notificationIds);

    /**
     * Look up an event from the notification store by id.
     *
     * @param eventId the event id
     * @return the IotasEvent
     */
    IotasEvent getEvent(String eventId);

    /**
     * Look up events from the notification store based on event id.
     */
    List<IotasEvent> getEvents(List<String> eventIds);


    /**
     * <p>
     * Returns a list of entities from the store based on some criteria.
     * E.g List of Notification for dataSourceId = 100
     *     List of latest 10 IotasEvent etc
     * </p>
     * @return the entities
     */
    <T> List<T> findEntities(Criteria<T> criteria);

    /**
     * Close connections with the data store and clean up.
     */
    void close();

    /**
     * Update the notification status of the notification.
     */
    Notification updateNotificationStatus(String notificationId, Notification.Status status);
}
