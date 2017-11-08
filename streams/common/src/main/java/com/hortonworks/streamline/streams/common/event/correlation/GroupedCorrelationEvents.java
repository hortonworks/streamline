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

import com.hortonworks.streamline.streams.common.event.EventInformation;

import java.util.*;

public class GroupedCorrelationEvents {
    private Map<String, EventInformation> allEvents;
    private Map<String, ComponentGroupedEvents> componentGroupedEvents;

    public GroupedCorrelationEvents(Map<String, EventInformation> correlatedEvents) {
        this.allEvents = correlatedEvents;

        this.componentGroupedEvents = new HashMap<>();

        correlatedEvents.values().forEach(event -> {
            String sourceComponentName = event.getComponentName();
            String targetComponentName = event.getTargetComponentName();

            ComponentGroupedEvents groupedEvents = componentGroupedEvents.get(sourceComponentName);
            if (groupedEvents == null) {
                groupedEvents = new ComponentGroupedEvents(sourceComponentName);
                componentGroupedEvents.put(sourceComponentName, groupedEvents);
            }
            groupedEvents.addOutputEventId(event.getEventId());

            groupedEvents = componentGroupedEvents.get(targetComponentName);
            if (groupedEvents == null) {
                groupedEvents = new ComponentGroupedEvents(targetComponentName);
                componentGroupedEvents.put(targetComponentName, groupedEvents);
            }
            groupedEvents.addInputEventId(event.getEventId());
        });
    }

    public Map<String, EventInformation> getAllEvents() {
        return allEvents;
    }

    public Map<String, ComponentGroupedEvents> getComponentGroupedEvents() {
        return componentGroupedEvents;
    }

    private static class ComponentGroupedEvents {
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
}
