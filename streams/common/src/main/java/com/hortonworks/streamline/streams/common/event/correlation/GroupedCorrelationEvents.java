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

import com.google.common.annotations.VisibleForTesting;
import com.hortonworks.streamline.streams.common.event.EventInformation;

import java.util.*;

import static java.util.stream.Collectors.toMap;

public class GroupedCorrelationEvents {
    private Map<String, EventInformation> allEvents;
    private Map<String, SortedComponentGroupedEvents> componentGroupedEvents;

    public GroupedCorrelationEvents(Map<String, EventInformation> correlatedEvents, String rootEventId) {
        this.allEvents = correlatedEvents;

        Map<String, ComponentGroupedEvents> unsortedComponentGroupedEvents = new HashMap<>();

        correlatedEvents.values().forEach(event -> {
            String sourceComponentName = event.getComponentName();
            String targetComponentName = event.getTargetComponentName();

            ComponentGroupedEvents groupedEvents = unsortedComponentGroupedEvents.get(sourceComponentName);
            if (groupedEvents == null) {
                groupedEvents = new ComponentGroupedEvents(sourceComponentName);
                unsortedComponentGroupedEvents.put(sourceComponentName, groupedEvents);
            }
            groupedEvents.addOutputEventId(event.getEventId());

            groupedEvents = unsortedComponentGroupedEvents.get(targetComponentName);
            if (groupedEvents == null) {
                groupedEvents = new ComponentGroupedEvents(targetComponentName);
                unsortedComponentGroupedEvents.put(targetComponentName, groupedEvents);
            }
            groupedEvents.addInputEventId(event.getEventId());
        });

        componentGroupedEvents = unsortedComponentGroupedEvents.entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey,
                        entry -> new SortedComponentGroupedEvents(allEvents, entry.getValue(), rootEventId)));
    }

    public Map<String, EventInformation> getAllEvents() {
        return allEvents;
    }

    public Map<String, SortedComponentGroupedEvents> getComponentGroupedEvents() {
        return componentGroupedEvents;
    }

    public static class ComponentGroupedEvents {
        private String componentName;
        private Set<String> inputEventIds;
        private Set<String> outputEventIds;

        public ComponentGroupedEvents(String componentName) {
            this.componentName = componentName;
            this.inputEventIds = new HashSet<>();
            this.outputEventIds = new HashSet<>();
        }

        public ComponentGroupedEvents addInputEventId(String eventId) {
            inputEventIds.add(eventId);
            return this;
        }

        public ComponentGroupedEvents addOutputEventId(String eventId) {
            outputEventIds.add(eventId);
            return this;
        }

        public String getComponentName() {
            return componentName;
        }

        public Set<String> getInputEventIds() {
            return inputEventIds;
        }

        public Set<String> getOutputEventIds() {
            return outputEventIds;
        }
    }

    public static class SortedComponentGroupedEvents {
        private String componentName;
        private boolean containingSelectedEvent;
        private List<String> inputEventIds;
        private List<String> outputEventIds;

        public SortedComponentGroupedEvents(Map<String, EventInformation> allEvents,
                                            ComponentGroupedEvents groupedEvents, String selectedEventId) {
            this.componentName = groupedEvents.getComponentName();

            this.inputEventIds = new LinkedList<>(groupedEvents.getInputEventIds());
            this.outputEventIds = new LinkedList<>(groupedEvents.getOutputEventIds());

            rearrangeEventIds(allEvents, selectedEventId, this.inputEventIds);
            rearrangeEventIds(allEvents, selectedEventId, this.outputEventIds);

            // check that the component is a source, and contains selected event id as source
            if (this.inputEventIds.size() == 0 && this.outputEventIds.size() > 0 &&
                    this.outputEventIds.get(0).equals(selectedEventId)) {
                this.containingSelectedEvent = true;
            }

        }

        private void rearrangeEventIds(Map<String, EventInformation> allEvents, String selectedEventId, List<String> eventIds) {
            if (eventIds.size() <= 1) {
                // no need to rearrange
                return;
            }

            List<String> movingIds = new LinkedList<>();

            Iterator<String> iter = eventIds.iterator();
            while (iter.hasNext()) {
                String eventId = iter.next();

                EventInformation eventInformation = allEvents.get(eventId);
                if (eventInformation == null) {
                    return;
                }

                if (eventId.equals(selectedEventId)) {
                    iter.remove();
                    // place first among moving ids
                    movingIds.add(0, eventId);
                } else if (eventInformation.getRootIds().contains(selectedEventId)) {
                    // check if the event contains selected event in root ids
                    // if then place them to just right side of selected event
                    iter.remove();
                    movingIds.add(eventId);
                }
            }

            eventIds.addAll(0, movingIds);
        }

        public String getComponentName() {
            return componentName;
        }

        public boolean isContainingSelectedEvent() {
            return containingSelectedEvent;
        }

        public List<String> getInputEventIds() {
            return inputEventIds;
        }

        public List<String> getOutputEventIds() {
            return outputEventIds;
        }
    }
}
