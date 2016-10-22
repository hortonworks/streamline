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
package org.apache.streamline.streams.runtime.splitjoin;

import org.apache.streamline.streams.StreamlineEvent;
import org.apache.streamline.streams.Result;
import org.apache.streamline.streams.common.StreamlineEventImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * This class broadcasts the received event to all the output streams. This can be extended to customize split logic.
 */
public class DefaultSplitter implements Splitter {

    public DefaultSplitter() {
    }

    @Override
    public List<Result> splitEvent(StreamlineEvent inputEvent, Set<String> outputStreams) {
        List<Result> results = new ArrayList<>();
        String groupId = getGroupId(inputEvent);
        int curPartNo = 0;
        int totalParts = outputStreams.size();
        for (String stream : outputStreams) {
            StreamlineEvent partitionedEvent = createPartitionEvent(inputEvent, groupId, ++curPartNo, stream, totalParts);
            results.add(new Result(stream, Collections.singletonList(partitionedEvent)));
        }

        return results;
    }

    private StreamlineEvent createPartitionEvent(StreamlineEvent event, String groupId, int partNo, String stream, int totalParts) {
        Map<String, Object> headers = new HashMap<>();
        if (event.getHeader() != null) {
            headers.putAll(event.getHeader());
        }
        headers.put(SplitActionRuntime.SPLIT_GROUP_ID, groupId);
        headers.put(SplitActionRuntime.SPLIT_PARTITION_ID, partNo);
        headers.put(SplitActionRuntime.SPLIT_TOTAL_PARTITIONS_ID, totalParts);
        return new StreamlineEventImpl(event.getFieldsAndValues(), event.getDataSourceId(),
                UUID.randomUUID().toString(), headers, stream, event.getAuxiliaryFieldsAndValues());
    }

    /**
     * @return groupid for a given {@code event}
     */
    protected String getGroupId(StreamlineEvent event) {
        return UUID.randomUUID().toString();
    }

}
