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


package com.hortonworks.streamline.streams.notification.store.hbase.mappers;

import com.hortonworks.streamline.streams.notification.Notification;
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
        put.addColumn(CF_NOTIFICATION_ID, notification.getId().getBytes(CHARSET), CV_DEFAULT);
    }

    @Override
    protected String getNotificationId(Result result) {
        return Bytes.toString(result.getFamilyMap(CF_NOTIFICATION_ID).firstEntry().getKey());
    }

    /**
     * Returns suffix for notification index.
     */
    protected String getIndexSuffix(Notification notification) {
        return new StringBuilder()
                .append(notification.getTs())
                .append(ROWKEY_SEP)
                .append(getUniqueIndexSuffix(notification))
                .toString();
    }

    /**
     * Returns the last INDEX_KEY_SUFFIX_LEN chars of notification.id
     * This is added as a suffix in the index table so that
     * notifications with same ts ends up as unique row in the index table.
     */
    protected final String getUniqueIndexSuffix(Notification notification) {
        int startIndex = Math.max(0, notification.getId().length() - INDEX_KEY_SUFFIX_LEN);
        return notification.getId().substring(startIndex, notification.getId().length());
    }
}
