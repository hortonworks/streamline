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


package com.hortonworks.streamline.streams.notification.service;

import com.hortonworks.streamline.streams.notification.Notification;
import com.hortonworks.streamline.streams.notification.Notifier;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    private final ConcurrentHashMap<String, Pair<NotificationQueueTask, Future<?>>> taskMap;

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


    public Future<?> enqueue(Notifier notifier, Notification notification) {
        NotificationQueueTask task = new NotificationQueueTask(notifier, notification);
        Future<?> future = executorService.submit(task);
        taskMap.put(notification.getId(), Pair.of(task, future));
        return future;
    }

    /**
     * Attempt re-delivery of a previously enqueued notification.
     *
     * @param notificationId id of a previously submitted notification.
     */
    public void resubmit(String notificationId) {
        Pair<NotificationQueueTask, Future<?>> taskStatus = taskMap.get(notificationId);
        if (taskStatus == null) {
            throw new NotificationServiceException("Could not find a previously enqueued task" +
                                                           " for notification id " + notificationId);
        } else if (!taskStatus.getValue().isDone()) {
            throw new NotificationServiceException("Previously enqueued task" +
                    " for notification id " + notificationId + " is not done");

        }
        Future<?> future = executorService.submit(taskStatus.getKey());
        taskMap.put(notificationId, Pair.of(taskStatus.getKey(), future));
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
