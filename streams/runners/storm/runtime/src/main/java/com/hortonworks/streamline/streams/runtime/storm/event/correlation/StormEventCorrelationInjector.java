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
package com.hortonworks.streamline.streams.runtime.storm.event.correlation;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.event.correlation.EventCorrelationInjector;
import com.hortonworks.streamline.streams.storm.common.StormTopologyUtil;
import org.apache.storm.tuple.Tuple;

import java.util.List;
import java.util.stream.Collectors;

public class StormEventCorrelationInjector {

    public StreamlineEvent injectCorrelationInformation(List<Object> tuple, List<Tuple> parentTuples, String componentName) {
        EventCorrelationInjector eventCorrelationInjector = new EventCorrelationInjector();
        return eventCorrelationInjector.injectCorrelationInformation(
                getStreamlineEventFromValues(tuple),
                parentTuples.stream().map(this::getStreamlineEventFromTuple).collect(Collectors.toList()),
                StormTopologyUtil.extractStreamlineComponentName(componentName));
    }

    private StreamlineEvent getStreamlineEventFromTuple(Tuple tuple) {
        final Object event = tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
        if (event instanceof StreamlineEvent) {
            return (StreamlineEvent) event;
        }

        throw new IllegalArgumentException("Invalid tuple received. Tuple [" + tuple + "] Event [" + event + "]");
    }

    private StreamlineEvent getStreamlineEventFromValues(List<Object> tuple) {
        final Object eventObj = tuple.get(0);

        if (eventObj instanceof StreamlineEvent) {
            return (StreamlineEvent) eventObj;
        }

        throw new IllegalArgumentException("Invalid Values received. Values [" + tuple + "] Event [" + eventObj + "]");
    }
}
