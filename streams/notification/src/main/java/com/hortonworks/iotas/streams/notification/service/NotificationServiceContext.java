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

import com.hortonworks.iotas.streams.notification.common.Notification;
import com.hortonworks.iotas.streams.notification.common.NotificationContext;
import com.hortonworks.iotas.streams.notification.common.NotifierConfig;
import com.hortonworks.iotas.streams.notification.store.NotificationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A wrapper context used by the {@link NotificationService} to track the retry attempts
 * and update the status in the {@link NotificationStore}
 */
public class NotificationServiceContext implements NotificationContext {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationServiceContext.class);
    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final String PROP_RETRY_COUNT = "maxAttempts";

    private final NotificationContext wrappedContext;
    private final NotificationQueueHandler queueHandler;
    private final NotificationService notificationService;
    private final ConcurrentHashMap<String, AtomicInteger> notificationMap;
    private final int maxAttempts;

    public NotificationServiceContext(NotificationContext context, NotificationQueueHandler queueHandler,
                                      NotificationService notificationService) {
        this.wrappedContext = context;
        this.queueHandler = queueHandler;
        this.notificationService = notificationService;
        this.notificationMap = new ConcurrentHashMap<>();
        Properties properties = context.getConfig().getProperties();
        String propRetryCount = null;
        if (properties != null) {
            propRetryCount = properties.getProperty(PROP_RETRY_COUNT);
        }
        this.maxAttempts = (propRetryCount != null) ? Integer.parseInt(propRetryCount) : DEFAULT_RETRY_COUNT;
    }

    @Override
    public NotifierConfig getConfig() {
        return wrappedContext.getConfig();
    }

    @Override
    public void ack(String notificationId) {
        LOG.debug("Updating status to DELIVERED for notification id {}", notificationId);
        notificationService.updateNotificationStatus(notificationId, Notification.Status.DELIVERED);
        notificationMap.remove(notificationId);
        queueHandler.remove(notificationId);
        wrappedContext.ack(notificationId);
    }

    @Override
    public void fail(String notificationId) {
        int attempt = currentAttempt(notificationId);
        LOG.info("Attempt [{}] failed. [{}] retries left.", attempt, maxAttempts - attempt);
        if (attempt >= maxAttempts) {
            LOG.info("Updating status to FAILED for notification id {}", notificationId);
            notificationService.updateNotificationStatus(notificationId, Notification.Status.FAILED);
            notificationMap.remove(notificationId);
            queueHandler.remove(notificationId);
            wrappedContext.fail(notificationId);
        } else {
            // queue it again
            queueHandler.resubmit(notificationId);
        }
    }

    @Override
    public String toString() {
        return "NotificationServiceContext{" +
                "wrappedContext=" + wrappedContext +
                ", maxAttempts=" + maxAttempts +
                '}';
    }

    private int currentAttempt(String notificationId) {
        AtomicInteger attempt = notificationMap.get(notificationId);
        if (attempt == null) {
            attempt = notificationMap.putIfAbsent(notificationId, new AtomicInteger(1));
        }
        if (attempt != null) {
            return attempt.incrementAndGet();
        }
        return 1;
    }
}
