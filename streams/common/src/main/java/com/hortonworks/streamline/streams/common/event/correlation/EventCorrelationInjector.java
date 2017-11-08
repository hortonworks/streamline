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
package com.hortonworks.streamline.streams.common.event.correlation;

import com.google.common.collect.ImmutableMap;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EventCorrelationInjector {

    public static final String HEADER_KEY_ROOT_IDS = "rootIds";
    public static final String HEADER_KEY_PARENT_IDS = "parentIds";
    public static final String HEADER_KEY_SOURCE_COMPONENT_NAME = "sourceComponentName";

    public StreamlineEvent injectCorrelationInformation(StreamlineEvent event,
                                                        List<StreamlineEvent> parentEvents, String componentName) {
        Set<String> rootIds = new HashSet<>();
        Set<String> parentIds = new HashSet<>();

        if (!parentEvents.isEmpty()) {
            parentEvents.forEach(parentEvent -> {
                Set<String> rootIdsForParent = EventCorrelationInjector.getRootIds(parentEvent);
                if (rootIdsForParent != null && !rootIdsForParent.isEmpty()) {
                    rootIds.addAll(rootIdsForParent);
                } else {
                    rootIds.add(parentEvent.getId());
                }

                parentIds.add(parentEvent.getId());
            });
        }

        // adding correlation and parent events information
        Map<String, Object> header = new HashMap<>();
        header.putAll(event.getHeader());
        header.put(HEADER_KEY_ROOT_IDS, rootIds);
        header.put(HEADER_KEY_PARENT_IDS, parentIds);
        header.put(HEADER_KEY_SOURCE_COMPONENT_NAME, componentName);

        return StreamlineEventImpl.builder()
                .from(event)
                .header(header)
                .build();
    }

    public static Set<String> getRootIds(StreamlineEvent event) {
        if (!event.getHeader().containsKey(HEADER_KEY_ROOT_IDS)) {
            throw new IllegalArgumentException("Root ID list is not in header.");
        }
        return (Set<String>) event.getHeader().get(HEADER_KEY_ROOT_IDS);
    }

    public static Set<String> getParentIds(StreamlineEvent event) {
        if (!event.getHeader().containsKey(HEADER_KEY_PARENT_IDS)) {
            throw new IllegalArgumentException("Parent ID list is not in header.");
        }
        return (Set<String>) event.getHeader().get(HEADER_KEY_PARENT_IDS);
    }

    public static String getSourceComponentName(StreamlineEvent event) {
        if (!event.getHeader().containsKey(HEADER_KEY_SOURCE_COMPONENT_NAME)) {
            throw new IllegalArgumentException("Source component name is not in header.");
        }
        return (String) event.getHeader().get(HEADER_KEY_SOURCE_COMPONENT_NAME);
    }
}
