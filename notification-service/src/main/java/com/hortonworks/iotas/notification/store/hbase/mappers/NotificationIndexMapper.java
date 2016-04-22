package com.hortonworks.iotas.notification.store.hbase.mappers;

import com.hortonworks.iotas.notification.Notification;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;


/**
 * A base class for Notification index table mappers.
 */
public abstract class NotificationIndexMapper extends AbstractNotificationMapper implements IndexMapper<Notification> {

    private static final byte[] CF_NOTIFICATION_ID = "ni".getBytes(CHARSET);

    /**
     * the number of characters to add as suffix in the index table row key to handle multiple
     * notifications with same ts.
     */
    private static final int INDEX_KEY_SUFFIX_LEN = 4;

    @Override
    protected void addColumns(Put put, Notification notification) {
        super.addColumns(put, notification);
        put.add(CF_NOTIFICATION_ID, notification.getId().getBytes(CHARSET), CV_DEFAULT);
    }

    @Override
    protected String getNotificationId(Result result) {
        return Bytes.toString(result.getFamilyMap(CF_NOTIFICATION_ID).firstEntry().getKey());
    }


    /**
     * Returns the last INDEX_KEY_SUFFIX_LEN chars of notification.id
     * This is added as a suffix in the index table so that
     * notifications with same ts ends up as unique row in the index table.
     */
    protected final String getIndexSuffix(Notification notification) {
        int startIndex = Math.max(0, notification.getId().length() - INDEX_KEY_SUFFIX_LEN);
        return notification.getId().substring(startIndex, notification.getId().length());
    }
}
