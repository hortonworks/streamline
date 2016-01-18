package com.hortonworks.iotas.notification.common;

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
    public enum Status {
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
