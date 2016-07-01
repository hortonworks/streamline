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

package com.hortonworks.iotas.streams.notification.service;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.streams.notification.common.Notification;
import com.hortonworks.iotas.streams.notification.common.NotificationContext;
import com.hortonworks.iotas.streams.notification.common.Notifier;
import com.hortonworks.iotas.streams.notification.common.NotifierConfig;
import com.hortonworks.iotas.service.CatalogService;

import java.util.List;

/**
 * <p>
 * This interface represents the basic services provided for IoTaS push
 * and pull notifications.
 * This could be plugged into any stream processing framework.
 * </p>
 */
public interface NotificationService {

    /**
     * Register a notifier object in the system. This method is supposed to
     * load the notifier from {@link NotifierConfig#getJarPath()} and associate it with the
     * given notifierName.
     *
     * @param notifierName the user provided notifierName by which we will refer the notifier.
     * @param ctx          the notification context to initialize the notifier with
     * @return the registered notifier
     */
    Notifier register(String notifierName, NotificationContext ctx);

    /**
     * De-registers a notifier if no longer needed
     *
     * @param notifierName the unique name of the notifier.
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
     * Gets notification from the store matching the given notification id.
     * </p>
     */
    Notification getNotification(String notificationId);

    /**
     * Return a list of notifications matching the given notification ids.
     */
    List<Notification> getNotifications(List<String> notificationIds);

    /**
     * Returns a list of notifications matching the query params. This would typically use
     * secondary indexes (e.g. HBase index tables) of the underlying implementation.
     */
    List<Notification> findNotifications(List<CatalogService.QueryParam> queryParams);

    /**
     * <p>
     * Gets Iotas event from the store matching the given eventId.
     * </p>
     */
    IotasEvent getEvent(String eventId);

    /**
     * Return a list of iotas events matching the given event ids.
     */
    List<IotasEvent> getEvents(List<String> eventIds);

    /**
     * Update the notification status.
     *
     * @param notificationId the notification id
     * @return the updated notification object.
     */
    Notification updateNotificationStatus(String notificationId, Notification.Status status);

    /**
     * Any clean up goes here
     */
    void close();
}
