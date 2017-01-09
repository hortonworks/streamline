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

package com.hortonworks.streamline.streams.notification.store;

import java.util.List;

/**
 * An interface for specifying different query criteria for querying the Notification store.
 * The interface is parameterized on the type of the entity (E.g. Notification, StreamlineEvent etc).
 */
public interface Criteria<T> {

    /**
     * The fields (key, value) pair that are specified in the criteria.
     */
    interface Field {

        String getName();
        String getValue();
    }
    /**
     * The class name of the entity that this query criteria is for.
     */
    Class<T> clazz();

    /**
     * The secondary field restrictions. (e.g. Notifications with notifier_name = "email_notifier").
     * If the fields are not indexed, it will result in a full table scan in implementations like HBase.
     */
    List<Field> fieldRestrictions();

    /**
     * The number of rows to return.
     */
    int numRows();

    /**
     * The start timestamp in millis.
     */
    long startTs();

    /**
     * The end timestamp in millis.
     */
    long endTs();

    /**
     * If the results should be in descending order
     */
    boolean isDescending();
}
