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

package com.hortonworks.streamline.streams.runtime.storm.testing;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.storm.common.StormTopologyUtil;
import org.apache.storm.task.TopologyContext;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OutputCollectorStreamlineEventLogger {
    private final TopologyContext topologyContext;
    private final TestRunEventLogger eventLogger;
    private final String componentName;

    public OutputCollectorStreamlineEventLogger(TopologyContext topologyContext, TestRunEventLogger eventLogger) {
        this.topologyContext = topologyContext;
        this.eventLogger = eventLogger;
        this.componentName = StormTopologyUtil.extractStreamlineComponentName(topologyContext.getThisComponentId());
    }

    public List<Integer> emitWithLoggingEvent(String streamId, List<Object> tuple, Function<List<Object>, List<Integer>> emitFunc) {
        List<Integer> tasks = emitFunc.apply(tuple);
        logEvent(streamId, tasks, tuple);
        return tasks;
    }

    public void emitDirectWithLoggingEvent(int taskId, String streamId, List<Object> tuple, Consumer<List<Object>> emitFunc) {
        emitFunc.accept(tuple);
        logEvent(streamId, Collections.singletonList(taskId), tuple);
    }

    private void logEvent(String streamId, List<Integer> taskIds, List<Object> tuple) {
        long now = System.currentTimeMillis();
        StreamlineEvent streamlineEvent = (StreamlineEvent) tuple.get(0);

        // will be more efficient if we split this to two streams, but it shouldn't be a big deal anyway
        Set<String> targetComponentIds = taskIds.stream()
                .map(topologyContext::getComponentId)
                .map(StormTopologyUtil::extractStreamlineComponentName)
                .collect(Collectors.toSet());

        eventLogger.writeEvent(now, componentName, streamId, targetComponentIds, streamlineEvent);
    }
}
