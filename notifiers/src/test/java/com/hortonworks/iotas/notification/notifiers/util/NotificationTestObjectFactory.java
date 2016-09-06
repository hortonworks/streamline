package com.hortonworks.iotas.notification.notifiers.util;

import com.google.common.collect.Lists;
import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationImpl;

import java.util.*;

public class NotificationTestObjectFactory {

    private NotificationTestObjectFactory() {

    }

    public static Notification getOne() {
        return getOne(null);
    }

    public static Notification getOne(Long timestamp) {
        Map<String, Object> fieldAndValues = new HashMap<>();
        fieldAndValues.put("one", "A");

        NotificationImpl.Builder builder = new NotificationImpl.Builder(fieldAndValues)
                .dataSourceIds(Arrays.asList("dsrcid-1"))
                .eventIds(Arrays.asList("eventid-1"))
                .notifierName("testsalil-notifier")
                .ruleId("rule-1")
                .status(Notification.Status.DELIVERED);

        if (timestamp != null) {
            builder.timestamp(timestamp);
        }

        return builder.build();
    }

    public static Notification applyStatus(Notification notification, Notification.Status newStatus) {
        return new NotificationImpl.Builder(notification.getFieldsAndValues())
                .id(notification.getId())
                .dataSourceIds(notification.getDataSourceIds())
                .eventIds(notification.getEventIds())
                .notifierName(notification.getNotifierName())
                .ruleId(notification.getRuleId())
                .timestamp(notification.getTs())
                .status(newStatus)
                .build();
    }

    public static List<Notification> getMany(int count) {
        List<Notification> notifications = Lists.newArrayList();
        for (int i = 0 ; i < count ; i++) {
            notifications.add(getOne());
        }
        return notifications;
    }

    public static List<Notification> getManyWithRandomTimestamp(int count) {
        Random random = new Random();

        List<Notification> notifications = Lists.newArrayList();
        for (int i = 0 ; i < count ; i++) {
            // should have same digit since it should be compared to dictionary order which we don't want
            notifications.add(getOne(System.currentTimeMillis() - random.nextInt(10000)));
        }
        return notifications;
    }
}
