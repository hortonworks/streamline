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
import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.streams.notification.common.Notification;
import com.hortonworks.iotas.streams.notification.common.NotificationContext;
import com.hortonworks.iotas.streams.notification.common.Notifier;
import com.hortonworks.iotas.streams.notification.store.NotificationStore;
import com.hortonworks.iotas.streams.notification.store.hbase.HBaseNotificationStore;
import com.hortonworks.iotas.streams.notification.store.CriteriaImpl;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.util.ProxyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Notification service implementation.
 */
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private static final String QUERY_PARAM_NUM_ROWS = "numRows";
    private static final String QUERY_PARAM_START_TS = "startTs";
    private static final String QUERY_PARAM_END_TS = "endTs";
    private static final String QUERY_PARAM_DESC = "desc";

    private static final String QUEUEHANDLER_THREADS = "queuehandler.threads";

    private final ConcurrentHashMap<String, Notifier> notifiers = new ConcurrentHashMap<>();

    private NotificationQueueHandler queueHandler;

    private final ProxyUtil<Notifier> notifierProxyUtil;

    /**
     * the underlying notification store.
     */
    private final NotificationStore notificationStore;

    /**
     * Uses HBaseNotificationStore by default.
     */
    public NotificationServiceImpl() {
        this(new HBaseNotificationStore());
    }

    public NotificationServiceImpl(NotificationStore store) {
        this(Collections.<String, Object>emptyMap(), store);
    }

    public NotificationServiceImpl(Map<String, Object> config, NotificationStore store) {
        LOG.info("Initializing NotificationServiceImpl with config {}", config);
        this.notificationStore = store;
        if(config.get(QUEUEHANDLER_THREADS) != null) {
            this.queueHandler = new NotificationQueueHandler(((Number)config.get(QUEUEHANDLER_THREADS)).intValue());
        } else {
            this.queueHandler = new NotificationQueueHandler();
        }
        this.notifierProxyUtil = new ProxyUtil<>(Notifier.class);
    }

    @Override
    public Notifier register(String notifierName, NotificationContext ctx) {
        // FIXME: change a way to load jar / class and modify NotificationServiceImplTest as relevant
        LOG.info("Registering notifier name {}, NotificationContext {}", notifierName, ctx);
        Notifier notifier = loadNotifier(ctx.getConfig().getJarPath(), ctx.getConfig().getClassName());
        Notifier registeredNotifier = notifiers.putIfAbsent(notifierName, notifier);
        if (registeredNotifier == null) {
            LOG.info("Initializing notifier");
            notifier.open(new NotificationServiceContext(ctx, queueHandler, this));
            registeredNotifier = notifier;
        }
        LOG.info("Notifier {} registered", notifierName);
        return registeredNotifier;
    }

    @Override
    public Notifier remove(String notifierName) {
        LOG.info("De-registering notifier {}", notifierName);
        Notifier notifier = notifiers.remove(notifierName);
        if (notifier != null) {
            LOG.info("Closing notifier {}", notifierName);
            notifier.close();
        }
        return notifier;
    }

    @Override
    public void notify(String notifierName, Notification notification) {
        LOG.debug("Notify notifierName {}, notification {}", notifierName, notification);
        notificationStore.store(notification);
        Notifier notifier = notifiers.get(notifierName);
        if (notifier == null) {
            throw new NoSuchNotifierException("Notifier not found for id " + notification.getNotifierName());
        }
        queueHandler.enqueue(notifier, notification);
    }

    @Override
    public Notification getNotification(String notificationId) {
        LOG.debug("getNotification with notificationId {}", notificationId);
        return notificationStore.getNotification(notificationId);
    }

    @Override
    public List<Notification> getNotifications(List<String> notificationIds) {
        LOG.debug("getNotifications with notificationIds {}", notificationIds);
        return notificationStore.getNotifications(notificationIds);
    }

    @Override
    public List<Notification> findNotifications(List<QueryParam> queryParams) {
        LOG.debug("findNotifications with queryParams {}", queryParams);
        CriteriaImpl<Notification> criteria = new CriteriaImpl<>(Notification.class);
        for (QueryParam qp : queryParams) {
            if (qp.name.equalsIgnoreCase(QUERY_PARAM_NUM_ROWS)) {
                criteria.setNumRows(Integer.parseInt(qp.value));
            } else if (qp.name.equals(QUERY_PARAM_START_TS)) {
                criteria.setStartTs(Long.parseLong(qp.value));
            } else if (qp.name.equals((QUERY_PARAM_END_TS))) {
                criteria.setEndTs(Long.parseLong(qp.value));
            } else if (qp.name.equals((QUERY_PARAM_DESC))) {
                criteria.setDescending(true);
            } else {
                criteria.addFieldRestriction(qp.name, qp.value);
            }
        }
        LOG.debug("Finding entities from notification store with criteria {}", criteria);
        return notificationStore.findEntities(criteria);
    }

    @Override
    public IotasEvent getEvent(String eventId) {
        LOG.debug("getEvent with eventId {}", eventId);
        return notificationStore.getEvent(eventId);
    }

    @Override
    public List<IotasEvent> getEvents(List<String> eventIds) {
        LOG.debug("getEvents with eventIds {}", eventIds);
        return notificationStore.getEvents(eventIds);
    }

    @Override
    public Notification updateNotificationStatus(String notificationId, Notification.Status status) {
        LOG.debug("updateNotificationStatus for notificationId {}, status {}", notificationId, status);
        return notificationStore.updateNotificationStatus(notificationId, status);
    }

    @Override
    public void close() {
        queueHandler.shutdown();
    }

    /**
     * Loads the jar from jarPath and instantiates {@link Notifier} specified in className.
     */
    private Notifier loadNotifier(String jarPath, String className) {
        try {
            LOG.info("Instantiating class {} via ProxyUtil", className);
            return this.notifierProxyUtil.loadClassFromJar(jarPath, className);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
