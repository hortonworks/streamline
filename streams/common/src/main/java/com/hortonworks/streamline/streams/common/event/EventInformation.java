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

import java.util.List;
import java.util.Map;
import java.util.Set;

public class EventInformation {
    private long timestamp;
    private String componentName;
    private String streamId;
    private Set<String> targetComponents;
    private String eventId;
    private Set<String> rootIds;
    private Set<String> parentIds;
    private Map<String, Object> fieldsAndValues;

    // jackson
    public EventInformation() {
    }

    public EventInformation(long timestamp, String componentName, String streamId, Set<String> targetComponents,
                            String eventId, Set<String> rootIds,
                            Set<String> parentIds, Map<String, Object> fieldsAndValues) {
        this.timestamp = timestamp;
        this.componentName = componentName;
        this.streamId = streamId;
        this.targetComponents = targetComponents;
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

    public Set<String> getTargetComponents() {
        return targetComponents;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventInformation)) return false;

        EventInformation that = (EventInformation) o;

        if (getTimestamp() != that.getTimestamp()) return false;
        if (getComponentName() != null ? !getComponentName().equals(that.getComponentName()) : that.getComponentName() != null)
            return false;
        if (getStreamId() != null ? !getStreamId().equals(that.getStreamId()) : that.getStreamId() != null)
            return false;
        if (getTargetComponents() != null ? !getTargetComponents().equals(that.getTargetComponents()) : that.getTargetComponents() != null)
            return false;
        if (getEventId() != null ? !getEventId().equals(that.getEventId()) : that.getEventId() != null) return false;
        if (getRootIds() != null ? !getRootIds().equals(that.getRootIds()) : that.getRootIds() != null) return false;
        if (getParentIds() != null ? !getParentIds().equals(that.getParentIds()) : that.getParentIds() != null)
            return false;
        return getFieldsAndValues() != null ? getFieldsAndValues().equals(that.getFieldsAndValues()) : that.getFieldsAndValues() == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (getTimestamp() ^ (getTimestamp() >>> 32));
        result = 31 * result + (getComponentName() != null ? getComponentName().hashCode() : 0);
        result = 31 * result + (getStreamId() != null ? getStreamId().hashCode() : 0);
        result = 31 * result + (getTargetComponents() != null ? getTargetComponents().hashCode() : 0);
        result = 31 * result + (getEventId() != null ? getEventId().hashCode() : 0);
        result = 31 * result + (getRootIds() != null ? getRootIds().hashCode() : 0);
        result = 31 * result + (getParentIds() != null ? getParentIds().hashCode() : 0);
        result = 31 * result + (getFieldsAndValues() != null ? getFieldsAndValues().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "EventInformation{" +
                "timestamp=" + timestamp +
                ", componentName='" + componentName + '\'' +
                ", streamId='" + streamId + '\'' +
                ", targetComponents='" + targetComponents + '\'' +
                ", eventId='" + eventId + '\'' +
                ", rootIds=" + rootIds +
                ", parentIds=" + parentIds +
                ", fieldsAndValues=" + fieldsAndValues +
                '}';
    }
}
