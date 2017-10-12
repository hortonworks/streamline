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

import java.util.ArrayList;
import java.util.List;

public class EventInformationTreeNode {
    private final EventInformation eventInformation;
    private final List<EventInformationTreeNode> children;

    public EventInformationTreeNode(EventInformation eventInformation) {
        this.eventInformation = eventInformation;
        this.children = new ArrayList<>();
    }

    public EventInformation getEventInformation() {
        return eventInformation;
    }

    public void addChild(EventInformationTreeNode child) {
        this.children.add(child);
    }

    public List<EventInformationTreeNode> getChildren() {
        return children;
    }
}
