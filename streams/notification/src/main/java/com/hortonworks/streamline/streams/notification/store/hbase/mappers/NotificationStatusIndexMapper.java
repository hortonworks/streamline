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

package com.hortonworks.streamline.streams.notification.store.hbase.mappers;

import com.hortonworks.streamline.streams.notification.Notification;
import com.hortonworks.streamline.streams.notification.common.NotificationImpl;
import org.apache.hadoop.hbase.client.Delete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Base class for all the index mappers whose index includes status (which is mutable).
 *
 */
public abstract class NotificationStatusIndexMapper extends NotificationIndexMapper {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationStatusIndexMapper.class);

    @Override
    public List<TableMutation> status(Notification notification, Notification.Status status) {
        // insert a new row
        Notification updated = new NotificationImpl.Builder(notification).status(status).build();
        List<TableMutation> tableMutations = tableMutations(updated);
        // delete existing
        for (byte[] rowKey : getRowKeys(notification)) {
            Delete delete = new Delete(rowKey);
            tableMutations.add(new TableMutationImpl(getTableName(), delete));
        }
        LOG.trace("TableMutations for status update {}", tableMutations);
        return tableMutations;
    }

    protected String getIndexSuffix(Notification notification) {
        return new StringBuilder()
                .append(notification.getStatus())
                .append(ROWKEY_SEP)
                .append(super.getIndexSuffix(notification))
                .toString();
    }
}
