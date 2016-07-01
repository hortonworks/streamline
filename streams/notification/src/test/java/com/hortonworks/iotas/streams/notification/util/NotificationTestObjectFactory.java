/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.hortonworks.iotas.streams.notification.util;

import com.google.common.collect.Lists;
import com.hortonworks.iotas.streams.notification.common.Notification;
import com.hortonworks.iotas.streams.notification.common.NotificationImpl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
                .notifierName("test-notifier")
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
