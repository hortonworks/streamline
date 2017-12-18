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
package com.hortonworks.streamline.streams.logsearch;

import java.util.List;

public class EventSearchResult {
    private Long matchedEvents;
    private List<Event> events;

    public EventSearchResult(Long matchedEvents, List<Event> events) {
        this.matchedEvents = matchedEvents;
        this.events = events;
    }

    public Long getMatchedEvents() {
        return matchedEvents;
    }

    public List<Event> getEvents() {
        return events;
    }

    public static class Event {
        private String appId;
        private String componentName;
        private String eventId;
        private String rootIds;
        private String parentIds;
        private String keyValues;
        private String headers;
        private String auxKeyValues;

        private long timestamp;

        public Event(String appId, String componentName, String eventId, String rootIds, String parentIds,
                     String keyValues, String headers, String auxKeyValues, long timestamp) {
            this.appId = appId;
            this.componentName = componentName;
            this.eventId = eventId;
            this.rootIds = rootIds;
            this.parentIds = parentIds;
            this.keyValues = keyValues;
            this.headers = headers;
            this.auxKeyValues = auxKeyValues;
            this.timestamp = timestamp;
        }

        public String getAppId() {
            return appId;
        }

        public String getComponentName() {
            return componentName;
        }

        public String getEventId() {
            return eventId;
        }

        public String getRootIds() {
            return rootIds;
        }

        public String getParentIds() {
            return parentIds;
        }

        public String getKeyValues() {
            return keyValues;
        }

        public String getHeaders() {
            return headers;
        }

        public String getAuxKeyValues() {
            return auxKeyValues;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}