/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/


package com.hortonworks.streamline.streams.notification.store;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.notification.Notification;

import java.util.List;
import java.util.Map;

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
     * Initialize the notification store
     * @param config the config
     */
    void init(Map<String, Object> config);

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
     * @return the StreamlineEvent
     */
    StreamlineEvent getEvent(String eventId);

    /**
     * Look up events from the notification store based on event id.
     */
    List<StreamlineEvent> getEvents(List<String> eventIds);


    /**
     * <p>
     * Returns a list of entities from the store based on some criteria.
     * E.g List of Notification for dataSourceId = 100
     *     List of latest 10 StreamlineEvent etc
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
