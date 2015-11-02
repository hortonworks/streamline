package com.hortonworks.iotas.notification.notifiers;

import com.hortonworks.iotas.notification.common.Notification;
import com.hortonworks.iotas.notification.common.NotificationContext;
import com.hortonworks.iotas.notification.common.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 * A sample notifier that just logs the message to the console.
 * Mainly for testing and debugging purpose.
 * </p>
 */
public class ConsoleNotifier implements Notifier {
    private static final Logger LOG = LoggerFactory.getLogger(ConsoleNotifier.class);
    private NotificationContext ctx;

    private void print(String msg) {
        LOG.info(msg);
        System.out.println();
    }

    @Override
    public void open(NotificationContext ctx) {
        print("Open called with NotificationContext " + ctx);
        this.ctx = ctx;
    }

    @Override
    public void notify(Notification notification) {
        print("Notify called with Notification " + notification);
        ctx.ack(notification.getId());
        print("Acked the notification");
    }

    @Override
    public void close() {
        print("Cleaning up!!");
    }

    @Override
    public boolean isPull() {
        return false;
    }

    @Override
    public List<String> getFields() {
        return Collections.emptyList();
    }
}
