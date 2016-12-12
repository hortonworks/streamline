package org.apache.streamline.streams.notification.store;

import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.notification.Notification;
import org.apache.streamline.streams.notification.common.NotificationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InMemoryNotificationStore implements NotificationStore {
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryNotificationStore.class);

    private final Map<String, Notification> notifications = new HashMap<>();

    @Override
    public void init(Map<String, Object> config) {
        LOG.info("Initializing InMemoryNotificationStore");
    }

    @Override
    public void store(Notification notification) {
        notifications.put(notification.getId(), notification);
    }

    @Override
    public Notification getNotification(String notificationId) {
        return notifications.get(notificationId);
    }

    @Override
    public List<Notification> getNotifications(List<String> notificationIds) {
        return notificationIds.stream().map(notifications::get).collect(Collectors.toList());
    }

    @Override
    public StreamlineEvent getEvent(String eventId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<StreamlineEvent> getEvents(List<String> eventIds) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> List<T> findEntities(Criteria<T> criteria) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public Notification updateNotificationStatus(String notificationId, Notification.Status status) {
        Notification current = notifications.get(notificationId);
        if (current == null) {
            throw new IllegalArgumentException("Notification with notificationId: " + notificationId + " does not exist.");
        }
        Notification updated = new NotificationImpl.Builder(current).status(status).build();
        notifications.put(notificationId, updated);
        return updated;
    }
}
