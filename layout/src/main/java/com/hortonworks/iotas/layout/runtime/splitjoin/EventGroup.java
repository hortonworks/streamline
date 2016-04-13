/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.layout.runtime.splitjoin;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.hortonworks.iotas.common.IotasEvent;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This class stores all split events of a specific split group.
 */
public class EventGroup {
    private final Cache<Integer, IotasEvent> splitEvents;
    private final String groupId;
    private final String dataSourceId;
    private final long eventExpiryInterval;

    private int totalPartitionEvents = -1;

    public EventGroup(String groupId, String dataSourceId, long eventExpiryInterval) {
        this.groupId = groupId;
        this.dataSourceId = dataSourceId;
        this.eventExpiryInterval = eventExpiryInterval;

        splitEvents =
                CacheBuilder.newBuilder()
                .expireAfterWrite(eventExpiryInterval, TimeUnit.MILLISECONDS).build();
    }

    public void addPartitionEvent(IotasEvent partitionedEvent) {
        final Map<String, Object> header = partitionedEvent.getHeader();
        if(header == null || !header.containsKey(SplitActionRuntime.SPLIT_PARTITION_ID)) {
            throw new IllegalArgumentException("Received event is not of partition event as it doe not contain header  with name: "+SplitActionRuntime.SPLIT_PARTITION_ID);
        }

        splitEvents.put((Integer) header.get(SplitActionRuntime.SPLIT_PARTITION_ID), partitionedEvent);
        if(header.get(SplitActionRuntime.SPLIT_TOTAL_PARTITIONS_ID) != null) {
            int x = (Integer) header.get(SplitActionRuntime.SPLIT_TOTAL_PARTITIONS_ID);
            if(totalPartitionEvents < x) {
                totalPartitionEvents = x;
            }
        }

    }

    public boolean isComplete() {
        return splitEvents.size() == totalPartitionEvents;
    }

    public String getDataSourceId() {
        return dataSourceId;
    }

    public String getGroupId() {
        return groupId;
    }

    public Iterable<IotasEvent> getSplitEvents() {
        return Collections.unmodifiableCollection(splitEvents.asMap().values());
    }

    @Override
    public String toString() {
        return "EventGroup{" +
                "partitionedEvents=" + splitEvents +
                ", groupId='" + groupId + '\'' +
                ", dataSourceId='" + dataSourceId + '\'' +
                ", totalPartitionEvents=" + totalPartitionEvents +
                '}';
    }
}
