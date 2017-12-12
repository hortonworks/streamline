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

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.event.correlation.EventCorrelationInjector;

import java.util.Set;

public class EventInformationBuilder {
    public EventInformation build(long timestamp, String componentName, String streamId, Set<String> targetComponents,
                                  StreamlineEvent event) {
        String sourceComponentName = EventCorrelationInjector.getSourceComponentName(event);
        if (!componentName.equals(sourceComponentName)) {
            throw new IllegalStateException("component name in event correlation is different from provided component name");
        }
        return new EventInformation(timestamp, componentName, streamId, targetComponents, event.getId(),
                EventCorrelationInjector.getRootIds(event), EventCorrelationInjector.getParentIds(event),
                event);
    }
}
