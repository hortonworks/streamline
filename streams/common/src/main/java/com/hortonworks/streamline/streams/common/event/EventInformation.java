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
package com.hortonworks.streamline.streams.common.event;

import java.util.Map;
import java.util.Set;

public class EventInformation {
    private long timestamp;
    private String componentName;
    private String streamId;
    private String targetComponentName;
    private String eventId;
    private Set<String> rootIds;
    private Set<String> parentIds;
    private Map<String, Object> fieldsAndValues;

    // jackson
    public EventInformation() {
    }

    public EventInformation(long timestamp, String componentName, String streamId, String targetComponentName,
                            String eventId, Set<String> rootIds,
                            Set<String> parentIds, Map<String, Object> fieldsAndValues) {
        this.timestamp = timestamp;
        this.componentName = componentName;
        this.streamId = streamId;
        this.targetComponentName = targetComponentName;
        this.eventId = eventId;
        this.rootIds = rootIds;
        this.parentIds = parentIds;
        this.fieldsAndValues = fieldsAndValues;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getStreamId() {
        return streamId;
    }

    public String getTargetComponentName() {
        return targetComponentName;
    }

    public String getEventId() {
        return eventId;
    }

    public Set<String> getRootIds() {
        return rootIds;
    }

    public Set<String> getParentIds() {
        return parentIds;
    }

    public Map<String, Object> getFieldsAndValues() {
        return fieldsAndValues;
    }
}
