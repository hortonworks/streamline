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

package com.hortonworks.iotas.streams.notification.common;

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
