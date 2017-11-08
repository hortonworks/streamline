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
package com.hortonworks.streamline.streams.common.event.tree;

import com.hortonworks.streamline.streams.common.event.EventInformation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class EventInformationTreeBuilder {
    private final List<EventInformation> events;

    public EventInformationTreeBuilder(List<EventInformation> events) {
        this.events = events;
    }

    public EventInformationTreeNode constructEventTree(String rootEventId) {
        Map<String, EventInformationTreeNode> nodes = constructEventTreeAsMap(rootEventId);
        return nodes.get(rootEventId);
    }

    public EventInformationTreeNode constructEventTree(String rootEventId, String subRootEventId) {
        Map<String, EventInformationTreeNode> nodes = constructEventTreeAsMap(rootEventId);
        return nodes.get(subRootEventId);
    }

    public Map<String, EventInformationTreeNode> constructEventTreeAsMap(String rootEventId) {
        Stream<EventInformation> eventsAssociatedToRootEventStream = events.stream().filter(e -> {
            boolean isRootEvent = e.getEventId().equals(rootEventId);
            boolean containsRootEvent = e.getRootIds() != null && e.getRootIds().contains(rootEventId);
            return isRootEvent || containsRootEvent;
        });

        Map<String, EventInformationTreeNode> nodes = eventsAssociatedToRootEventStream.collect(
                toMap(EventInformation::getEventId, EventInformationTreeNode::new));

        nodes.forEach((eventId, node) -> {
            Set<String> parentIds = node.getEventInformation().getParentIds();
            parentIds.forEach(parentId -> {
                EventInformationTreeNode parentNode = nodes.get(parentId);
                // parent will not be in nodes when parent is originated to another root event
                // if it's the case, just ignore it
                if (parentNode != null) {
                    parentNode.addChild(node);
                }
            });
        });
        return nodes;
    }

}
