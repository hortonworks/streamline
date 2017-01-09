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

package com.hortonworks.streamline.streams.notification;

import java.util.List;
import java.util.Map;

/**
 * This represents the notification that is passed to
 * the notifiers from the notification service.
 */
public interface Notification {

    /**
     * The state of notification
     */
    enum Status {
        NEW, DELIVERED, FAILED
    }

    /**
     * <p>
     * A unique ID for this notification.
     * </p>
     *
     * @return the notification id
     */
    String getId();

    /**
     * <p>
     * The list of input event(s) Ids that triggered
     * the notification.
     * </p>
     *
     * @return the list of event Ids that resulted in this notification.
     */
    List<String> getEventIds();

    /**
     * <p>
     * The list of DataSource Ids from where the events originated that resulted
     * in the notification.
     * </p>
     *
     * @return the list of DataSource Ids from where the events originated that resulted
     * in the notification.
     */
    List<String> getDataSourceIds();

    /**
     * <p>
     * The rule-id that triggered this notification.
     * </p>
     *
     * @return the rule id that triggered this notification.
     */
    String getRuleId();

    /**
     * The status of the notification, i.e. if its new, delivered or failed.
     *
     * @return the status of the notification.
     */
    Status getStatus();

    /**
     * <p>The [key, value] pairs in the notification.
     * this will be used by the notifier to construct
     * the notification.
     * </p>
     *
     * @return the key, values in the notification object.
     */
    Map<String, Object> getFieldsAndValues();

    /**
     * The unique name of the notifier for which this notification is intended.
     *
     * @return the notifier name.
     */
    String getNotifierName();

    /**
     * The ts in millis when the notification is generated.
     */
    long getTs();
}
