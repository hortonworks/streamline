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

package org.apache.streamline.streams.notification.service;

import org.apache.streamline.streams.notification.Notification;
import org.apache.streamline.streams.notification.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Asynchronously delivers notifications to notifiers.
 */
public class NotificationQueueHandler {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationQueueHandler.class);
    private static final int MAX_THREADS = 10;
    /**
     * Track the tasks so that it can be re-submitted in case of retry.
     */
    private final ConcurrentHashMap<String, NotificationQueueTask> taskMap;

    private static class NotificationQueueTask implements Runnable {
        final Notifier notifier;
        final Notification notification;

        NotificationQueueTask(Notifier notifier, Notification notification) {
            this.notifier = notifier;
            this.notification = notification;
        }

        @Override
        public void run() {
            try {
                notifier.notify(notification);
            } catch (Throwable th) {
                LOG.error("Sending notification failed ", th);
                // fail so that the framework can retry
                notifier.getContext().fail(notification.getId());
                throw th;
            }
        }
    }

    private final ExecutorService executorService;

    public NotificationQueueHandler() {
        this(MAX_THREADS);
    }

    public NotificationQueueHandler(int nThreads) {
        // TODO: evaluate ThreadPoolExecutor with bounded queue size
        executorService = Executors.newFixedThreadPool(nThreads);
        taskMap = new ConcurrentHashMap<>();
    }


    public void enqueue(Notifier notifier, Notification notification) {
        NotificationQueueTask task = new NotificationQueueTask(notifier, notification);
        taskMap.put(notification.getId(), task);
        executorService.submit(task);
    }

    /**
     * Attempt re-delivery of a previously enqueued notification.
     *
     * @param notificationId id of a previously submitted notification.
     */
    public void resubmit(String notificationId) {
        NotificationQueueTask task = taskMap.get(notificationId);
        if (task == null) {
            throw new NotificationServiceException("Could not find a previously enqueued task" +
                                                           " for notification id " + notificationId);
        }
        executorService.submit(task);
    }

    public void remove(String notificationId) {
        taskMap.remove(notificationId);
    }

    public void shutdown() {
        LOG.info("Shutting down queue handler");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                    LOG.info("Executor service failed to shutdown");
                }
            }
        } catch (InterruptedException ex) {
            LOG.error("Got exception while awaiting termination", ex);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
