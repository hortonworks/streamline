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
package com.hortonworks.iotas.layout.runtime.splitjoin;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.common.Result;
import com.hortonworks.iotas.layout.design.rule.action.Action;
import com.hortonworks.iotas.layout.design.splitjoin.JoinAction;
import com.hortonworks.iotas.layout.runtime.RuntimeService;
import com.hortonworks.iotas.layout.runtime.rule.action.AbstractActionRuntime;
import com.hortonworks.iotas.layout.runtime.rule.action.ActionRuntime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * {@link ActionRuntime} implementation for {@link JoinAction}
 */
public class JoinActionRuntime extends AbstractActionRuntime {
    private Cache<String, EventGroup> groupedEvents;
    private final JoinAction joinAction;
    private Joiner joiner;

    public JoinActionRuntime(JoinAction joinAction) {
        this.joinAction = joinAction;
    }

    @Override
    public void initialize(Map<String, Object> config) {
        super.initialize(config);

        final Long jarId = joinAction.getJarId();
        final String joinerClassName = joinAction.getJoinerClassName();
        joiner = getInstance(jarId, joinerClassName, Joiner.class);
        if (joiner == null) {
            joiner = new DefaultJoiner();
        }

        groupedEvents = CacheBuilder.newBuilder()
                .expireAfterWrite(joinAction.getGroupExpiryInterval(), TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public List<Result> execute(IotasEvent iotasEvent) {
        // group received event if possible
        final EventGroup eventGroup = groupEvents(iotasEvent);

        // join them if group is complete
        if (eventGroup != null && eventGroup.isComplete()) {
            return joinEvents(eventGroup);
        }

        return null;
    }

    /**
     * Join all subevents and generate an event for the given output stream.
     *
     * @param eventGroup
     */
    protected List<Result> joinEvents(EventGroup eventGroup) {
        IotasEvent joinedEvent = joiner.join(eventGroup);

        List<Result> results = new ArrayList<>();
        for (String stream : getOutputStreams()) {
            results.add(new Result(stream, Collections.singletonList(getIotasEvent(joinedEvent, stream))));
        }
        groupedEvents.invalidate(eventGroup.getGroupId());

        return results;
    }

    private IotasEvent getIotasEvent(IotasEvent iotasEvent, String stream) {
        return new IotasEventImpl(iotasEvent.getFieldsAndValues(), iotasEvent.getDataSourceId(), iotasEvent.getId(), iotasEvent.getHeader(), stream, iotasEvent.getAuxiliaryFieldsAndValues());
    }

    protected EventGroup groupEvents(IotasEvent iotasEvent) {

        final Map<String, Object> header = iotasEvent.getHeader();
        if (header != null && header.containsKey(SplitActionRuntime.SPLIT_GROUP_ID)) {
            final String groupId = (String) header.get(SplitActionRuntime.SPLIT_GROUP_ID);
            final String dataSourceId = iotasEvent.getDataSourceId();
            final EventGroup eventGroup = getEventGroup(groupId, dataSourceId);
            eventGroup.addPartitionEvent(iotasEvent);

            return eventGroup;
        }

        return null;
    }

    private EventGroup getEventGroup(String groupId, String dataSourceId) {
        EventGroup eventGroup = groupedEvents.getIfPresent(groupId);
        if (eventGroup == null) {
            eventGroup = new EventGroup(groupId, dataSourceId, joinAction.getEventExpiryInterval());
            groupedEvents.put(groupId, eventGroup);
        }
        return eventGroup;
    }

    @Override
    public Set<String> getOutputStreams() {
        return joinAction.getOutputStreams();
    }

    public static class Factory implements RuntimeService.Factory<ActionRuntime, Action> {
        @Override
        public ActionRuntime create(Action action) {
            return new JoinActionRuntime((JoinAction) action);
        }
    }
}
