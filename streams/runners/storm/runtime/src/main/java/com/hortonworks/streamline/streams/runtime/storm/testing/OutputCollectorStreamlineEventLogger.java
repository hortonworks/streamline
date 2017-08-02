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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class OutputCollectorStreamlineEventLogger {
    private final TestRunEventLogger eventLogger;
    private final String componentName;

    public OutputCollectorStreamlineEventLogger(TestRunEventLogger eventLogger, String componentName) {
        this.eventLogger = eventLogger;
        this.componentName = componentName;
    }

    public List<Integer> emitWithLoggingEvent(String streamId, List<Object> tuple, Function<List<Object>, List<Integer>> emitFunc) {
        List<Integer> ret = emitFunc.apply(tuple);
        StreamlineEvent streamlineEvent = (StreamlineEvent) tuple.get(0);
        eventLogger.writeEvent(System.currentTimeMillis(), TestRunEventLogger.EventType.OUTPUT, componentName,
                streamId, streamlineEvent);
        return ret;
    }

    public void emitDirectWithLoggingEvent(String streamId, List<Object> tuple, Consumer<List<Object>> emitFunc) {
        emitFunc.accept(tuple);
        StreamlineEvent streamlineEvent = (StreamlineEvent) tuple.get(0);
        eventLogger.writeEvent(System.currentTimeMillis(), TestRunEventLogger.EventType.OUTPUT, componentName,
                streamId, streamlineEvent);
    }

}
