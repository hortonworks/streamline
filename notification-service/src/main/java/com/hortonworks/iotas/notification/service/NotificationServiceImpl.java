package com.hortonworks.iotas.notification.service;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationContext;
import com.hortonworks.iotas.notification.common.Notifier;
import com.hortonworks.iotas.notification.store.NotificationStore;
import com.hortonworks.iotas.notification.store.hbase.HBaseNotificationStore;
import com.hortonworks.util.ReflectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Notification service implementation.
 */
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final ConcurrentHashMap<String, Notifier> notifiers = new ConcurrentHashMap<>();
    private final NotificationStore notificationStore;

    public NotificationServiceImpl() {
        notificationStore = new HBaseNotificationStore();
    }

    @Override
    public Notifier register(String notifierName, NotificationContext ctx) {
        LOG.debug("Registering notifier name {}, NotificationContext {}", notifierName, ctx);
        Notifier notifier = loadNotifier(ctx.getConfig().getJarPath(), ctx.getConfig().getClassName());
        Notifier registeredNotifier = notifiers.putIfAbsent(notifierName, notifier);
        if(registeredNotifier == null) {
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
        if(notifier != null) {
            LOG.debug("Closing notifier {}", notifierName);
            notifier.close();
        }
        return notifier;
    }

    @Override
    public void notify(String notifierName, Notification notification) {
        LOG.debug("Notify notifierName {}, notification {}", notifierName, notification);
        notificationStore.store(notification);
        Notifier notifier = notifiers.get(notifierName);
        if(notifier == null) {
            throw new NoSuchNotifierException("Notifier not found for id " + notification.getNotifierName());
        }
        notifier.notify(notification);
    }

    @Override
    public List<Notification> getNotifications() {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

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
