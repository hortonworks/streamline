package com.hortonworks.iotas.notification.store;

import java.util.Map;

/**
 * An interface for specifying different query criteria for querying the Notification store.
 * The interface is parameterized on the type of the entity (E.g. Notification, IotasEvent etc).
 */
public interface Criteria<T> {
    /**
     * The class name of the entity that this query criteria is for.
     */
    Class<T> clazz();

    /**
     * The secondary field restrictions. (e.g. Notifications with notifier_name = "email_notifier").
     * If the fields are not indexed, it will result in a full table scan in implementations like HBase.
     */
    Map<String, String> fieldRestrictions();

    /**
     * The number of rows to return.
     */
    int numRows();
}
