/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.streamline.streams.runtime.splitjoin;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.Result;
import com.hortonworks.streamline.streams.layout.component.impl.splitjoin.SplitAction;
import com.hortonworks.streamline.streams.layout.component.rule.action.Action;
import com.hortonworks.streamline.streams.runtime.RuntimeService;
import com.hortonworks.streamline.streams.runtime.rule.action.ActionRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runtime for {@link SplitAction}
 */
public class SplitActionRuntime extends AbstractSplitJoinActionRuntime {

    private static final Logger log = LoggerFactory.getLogger(SplitActionRuntime.class);

    /**
     * Defined these constants for event headers to recognize whether an event is part of the split/join.
     * All the events created as part of split/stage/join will contain these headers.
     * Other way to do it is have new events like GroupRootEvent and PartitionEvent, avoiding that for now to
     * have a simple approach. Any Stage processor can not have split action in them.
     */
    public static final String SPLIT_GROUP_ID = "com.hortonworks.streamline.split.group_id";
    public static final String SPLIT_PARTITION_ID = "com.hortonworks.streamline.split.partition_id";
    public static final String SPLIT_TOTAL_PARTITIONS_ID = "com.hortonworks.streamline.split.partition.total.count";

    private final SplitAction splitAction;
    private Splitter splitter;

    public SplitActionRuntime(SplitAction splitAction) {
        this.splitAction = splitAction;
    }

    @Override
    public void initialize(Map<String, Object> config) {
        super.initialize(config);

        final Long jarId = splitAction.getJarId();
        final String splitterClassName = splitAction.getSplitterClassName();
        splitter = getInstance(jarId, splitterClassName, Splitter.class);
        if(splitter == null) {
            splitter = new DefaultSplitter();
        }
    }

    @Override
    public List<Result> execute(StreamlineEvent input) {
        // based on split-action configuration, generate events for respective streams
        final List<Result> results = splitter.splitEvent(input, getOutputStreams());

        // check whether the split event has all the required split/join info.
        for (Result result : results) {
            for (StreamlineEvent event : result.events) {
                checkGroupIdPartitionId(event);
            }
        }

        return results;
    }

    private void checkGroupIdPartitionId(StreamlineEvent event) {
        final Map<String, Object> header = event.getHeader();

        if (header == null) {
            log.error("Event [{}] does not have headers", event);
            throw new IllegalStateException("header can not be null for split events");
        }
        if (header.get(SPLIT_GROUP_ID) == null || header.get(SPLIT_PARTITION_ID) == null) {
            log.error("Event [{}] does not have complete split event info with group-id:[{}] and partition-id:[{}]", event, header.get(SPLIT_GROUP_ID), header.get(SPLIT_PARTITION_ID));
            throw new IllegalStateException("header should have group-id and partition-id for split events");
        }

    }

    @Override
    public Set<String> getOutputStreams() {
        return splitAction.getOutputStreams();
    }

    public static class Factory implements RuntimeService.Factory<ActionRuntime, Action> {

        @Override
        public ActionRuntime create(Action action) {
            return new SplitActionRuntime((SplitAction) action);
        }
    }
}
