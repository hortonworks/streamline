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

package org.apache.streamline.streams.notification.store.hbase.mappers;

import org.apache.streamline.streams.notification.Notification;

import java.util.Arrays;
import java.util.List;

/**
 * Secondary index mapping for ruleId + status to notification
 */
public class RuleStatusNotificationMapper extends NotificationStatusIndexMapper {
    /**
     * The HBase index table
     */
    private static final String TABLE_NAME = "Rule_Status_Notification";
    /**
     * The notification field that is indexed
     */
    private static final List<String> INDEX_FIELD_NAMES = Arrays.asList("ruleId", "status");

    @Override
    protected List<byte[]> getRowKeys(Notification notification) {
        return Arrays.asList(new StringBuilder(notification.getRuleId())
                                     .append(ROWKEY_SEP)
                                     .append(getIndexSuffix(notification))
                                     .toString().getBytes(CHARSET));
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public List<String> getIndexedFieldNames() {
        return INDEX_FIELD_NAMES;
    }
}
