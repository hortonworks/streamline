/**
 * Copyright 2017 Hortonworks.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.hortonworks.streamline.streams.common.event.correlation;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.hortonworks.streamline.streams.common.event.EventInformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class CorrelatedEventsGrouper {
    private final List<EventInformation> events;

    public CorrelatedEventsGrouper(List<EventInformation> events) {
        this.events = events;
    }

    /**
     * Returns the group of correlated events per source where the group of related source events
     * produces the same downstream events
     *
     * @param sources the set of source component names
     * @return the group of correlated events per source
     */
    public List<Set<String>> groupByRelatedSourceEvents(Set<String> sources) {
        Multimap<Set<String>, String> allEventsToSourceEvents = LinkedHashMultimap.create();
        Stream<String> rootEventIds = events.stream().filter(e -> e != null && e.getRootIds().isEmpty())
                .map(EventInformation::getEventId);
        rootEventIds.forEach(rootEventId -> {
            Map<String, EventInformation> allRelatedEvents = buildRelatedEventsMap(rootEventId);
            allEventsToSourceEvents.put(allRelatedEvents.keySet(), rootEventId);
        });

        List<Set<String>> result = new ArrayList<>();
        allEventsToSourceEvents.asMap().values().forEach(v ->
                result.add(new HashSet<>(v))
        );

        return result;
    }

    public GroupedCorrelationEvents groupByComponent(String rootEventId) {
        return new GroupedCorrelationEvents(buildRelatedEventsMap(rootEventId), rootEventId);
    }

    private Map<String, EventInformation> buildRelatedEventsMap(String rootEventId) {
        Map<String, EventInformation> allEventsMap = events.stream()
                .collect(toMap(EventInformation::getEventId, e -> e));

        Stream<EventInformation> eventsAssociatedToRootEventStream = events.stream().filter(e -> {
            boolean isRootEvent = e.getEventId().equals(rootEventId);
            boolean containsRootEvent = e.getRootIds() != null && e.getRootIds().contains(rootEventId);
            return isRootEvent || containsRootEvent;
        });

        Map<String, EventInformation> relatedEventsMap = eventsAssociatedToRootEventStream.collect(
                toMap(EventInformation::getEventId, e -> e));

        addNonExistingParents(allEventsMap, relatedEventsMap);

        return relatedEventsMap;
    }

    private void addNonExistingParents(Map<String, EventInformation> allEventsMap,
                                       Map<String, EventInformation> relatedEventsMap) {
        Map<String, EventInformation> eventsToAddMap = new HashMap<>();

        relatedEventsMap.forEach((eventId, event) ->
                findAndAddParentsIfNecessary(allEventsMap, relatedEventsMap, eventsToAddMap, event));

        // add all the events we newly found
        relatedEventsMap.putAll(eventsToAddMap);
    }

    private void findAndAddParentsIfNecessary(Map<String, EventInformation> allEventsMap,
                                              Map<String, EventInformation> relatedEventsMap,
                                              Map<String, EventInformation> eventsToAddMap,
                                              EventInformation event) {
        Set<String> parentIds = event.getParentIds();

        for (String parentId : parentIds) {
            if (!relatedEventsMap.containsKey(parentId) && !eventsToAddMap.containsKey(parentId)) {
                // find and add parent to relatedEventsMap
                EventInformation parent = allEventsMap.get(parentId);
                if (parent == null) {
                    throw new RuntimeException("Failed to find parent event: logged event information may be corrupted.");
                }
                eventsToAddMap.put(parent.getEventId(), parent);

                // newly found parent event may have parent event(s) as well... find them as well
                findAndAddParentsIfNecessary(allEventsMap, relatedEventsMap, eventsToAddMap, parent);
            }
        }
    }

}
