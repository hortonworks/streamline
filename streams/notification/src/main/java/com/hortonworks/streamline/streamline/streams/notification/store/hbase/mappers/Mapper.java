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

import org.apache.hadoop.hbase.client.Result;

import java.util.List;

/**
 * Maps the different entities like Notification,
 * StreamlineEvent, Rule etc into one or more HBase tables.
 */
public interface Mapper<T> {

    /**
     * The row key separator.
     */
    String ROWKEY_SEP = "|";

    /**
     * Returns a list of HBase table mutations for the entity type T
     */
    List<TableMutation> tableMutations(T t);

    /**
     * Maps the HBase row result back to entity.
     */
    T entity(Result result);

    /**
     * The mapping table name
     */
    String getTableName();

    /**
     * <p>
     * Return Column_Family, Column_Qualifier, Column_Value triplet for
     * an entity member name and value.
     * E.g. ["s".getBytes(), "qs".getBytes(), "NEW".getBytes()] for
     * {@code NotificationMapper.mapMemberValue("status", NEW)}.
     * If a memberName is not found, returns null.
     * </p>
     */
    List<byte[]> mapMemberValue(String memberName, String value);
}
