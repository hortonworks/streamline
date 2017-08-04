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

package com.hortonworks.streamline.streams.runtime.splitjoin;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.Result;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.layout.component.impl.splitjoin.JoinAction;
import com.hortonworks.streamline.streams.layout.component.rule.action.Action;
import com.hortonworks.streamline.streams.runtime.RuntimeService;
import com.hortonworks.streamline.streams.runtime.rule.action.ActionRuntime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * {@link ActionRuntime} implementation for {@link JoinAction}
 */
public class JoinActionRuntime extends AbstractSplitJoinActionRuntime {
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
    public List<Result> execute(StreamlineEvent event) {
        // group received event if possible
        final EventGroup eventGroup = groupEvents(event);

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
        StreamlineEvent joinedEvent = joiner.join(eventGroup);

        List<Result> results = new ArrayList<>();
        for (String stream : getOutputStreams()) {
            results.add(new Result(stream, Collections.singletonList(getStreamlineEvent(joinedEvent, stream))));
        }
        groupedEvents.invalidate(eventGroup.getGroupId());

        return results;
    }

    private StreamlineEvent getStreamlineEvent(StreamlineEvent event, String stream) {
        return new StreamlineEventImpl(event, event.getDataSourceId(), event.getId(), event.getHeader(), stream, event.getAuxiliaryFieldsAndValues());
    }

    protected EventGroup groupEvents(StreamlineEvent event) {

        final Map<String, Object> header = event.getHeader();
        if (header != null && header.containsKey(SplitActionRuntime.SPLIT_GROUP_ID)) {
            final String groupId = (String) header.get(SplitActionRuntime.SPLIT_GROUP_ID);
            final String dataSourceId = event.getDataSourceId();
            final EventGroup eventGroup = getEventGroup(groupId, dataSourceId);
            eventGroup.addPartitionEvent(event);

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

    public static class Factory implements RuntimeService.Factory<ActionRuntime, JoinAction> {
        @Override
        public ActionRuntime create(JoinAction action) {
            return new JoinActionRuntime(action);
        }
    }
}
