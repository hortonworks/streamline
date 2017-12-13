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

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;

import java.util.List;

/**
 * This class restricts emitting with not anchoring parent tuples,
 * which makes chaining output collectors with windowed bolt work.
 */
public class EventCorrelatingWindowedOutputCollector extends EventCorrelatingOutputCollector {

    public EventCorrelatingWindowedOutputCollector(TopologyContext topologyContext, OutputCollector delegate) {
        // we simply ignore the _delegate in OutputCollector and override all of the methods
        // this will work with subclass of OutputCollector since we only expose methods what we know about
        super(topologyContext, delegate);
    }

    @Override
    public List<Integer> emit(String streamId, List<Object> tuple) {
        throw new UnsupportedOperationException("Emitting with implicit anchoring is not support.");
    }

    @Override
    public List<Integer> emit(List<Object> tuple) {
        throw new UnsupportedOperationException("Emitting with implicit anchoring is not support.");
    }

    @Override
    public void emitDirect(int taskId, String streamId, List<Object> tuple) {
        throw new UnsupportedOperationException("Emitting with implicit anchoring is not support.");
    }

    @Override
    public void emitDirect(int taskId, List<Object> tuple) {
        throw new UnsupportedOperationException("Emitting with implicit anchoring is not support.");
    }
}
