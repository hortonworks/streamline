package com.hortonworks.iotas.notification.service;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationContext;
import com.hortonworks.iotas.notification.common.Notifier;
import com.hortonworks.iotas.notification.store.NotificationStore;
import com.hortonworks.iotas.notification.store.hbase.HBaseNotificationStore;
import com.hortonworks.iotas.notification.store.CriteriaImpl;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.util.ReflectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Notification service implementation.
 */
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private static final String QUERY_PARAM_NUM_ROWS = "numRows";
    private static final String QUERY_PARAM_START_TS = "startTs";
    private static final String QUERY_PARAM_END_TS = "endTs";

    private final ConcurrentHashMap<String, Notifier> notifiers = new ConcurrentHashMap<>();


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
        notificationStore = store;
    }

    @Override
    public Notifier register(String notifierName, NotificationContext ctx) {
        LOG.debug("Registering notifier name {}, NotificationContext {}", notifierName, ctx);
        Notifier notifier = loadNotifier(ctx.getConfig().getJarPath(), ctx.getConfig().getClassName());
        Notifier registeredNotifier = notifiers.putIfAbsent(notifierName, notifier);
        if (registeredNotifier == null) {
            LOG.debug("Initializing notifier");
            notifier.open(ctx);
            registeredNotifier = notifier;
        }
        LOG.debug("Notifier {} registered", notifierName);
        return registeredNotifier;
    }

    @Override
    public Notifier remove(String notifierName) {
        LOG.debug("De-registering notifier {}", notifierName);
        Notifier notifier = notifiers.remove(notifierName);
        if (notifier != null) {
            LOG.debug("Closing notifier {}", notifierName);
            notifier.close();
        }
        return notifier;
    }

    @Override
    public void notify(String notifierName, Notification notification) {
        LOG.debug("Notify notifierName {}, notification {}", notifierName, notification);
        // TODO: for better performance the store could be done asynchronously
        notificationStore.store(notification);
        Notifier notifier = notifiers.get(notifierName);
        if (notifier == null) {
            throw new NoSuchNotifierException("Notifier not found for id " + notification.getNotifierName());
        }
        notifier.notify(notification);
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
    public List<Notification> findNotifications(List<CatalogService.QueryParam> queryParams) {
        LOG.debug("findNotifications with queryParams {}", queryParams);
        CriteriaImpl<Notification> criteria = new CriteriaImpl<>(Notification.class);
        for (CatalogService.QueryParam qp : queryParams) {
            if (qp.name.equalsIgnoreCase(QUERY_PARAM_NUM_ROWS)) {
                criteria.setNumRows(Integer.parseInt(qp.value));
            } else if (qp.name.equals(QUERY_PARAM_START_TS)) {
                criteria.setStartTs(Long.parseLong(qp.value));
            } else if (qp.name.equals((QUERY_PARAM_END_TS))) {
                criteria.setEndTs(Long.parseLong(qp.value));
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

    /**
     * Loads the jar from jarPath and instantiates {@link Notifier} specified in className.
     */
    private Notifier loadNotifier(String jarPath, String className) {
        try {
            if (!ReflectionHelper.isJarInClassPath(jarPath) && !ReflectionHelper.isClassLoaded(className)) {
                LOG.info("Loading jar and all its classses from jar path {}", jarPath);
                ReflectionHelper.loadJarAndAllItsClasses(jarPath);
            }
            LOG.info("Instantiating classs {} via ReflectionHelper", className);
            return ReflectionHelper.newInstance(className);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
