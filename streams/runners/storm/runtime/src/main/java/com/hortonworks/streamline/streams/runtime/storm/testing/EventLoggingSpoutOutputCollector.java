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

import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.utils.Utils;

import java.util.List;

public class EventLoggingSpoutOutputCollector extends SpoutOutputCollector {
    private final SpoutOutputCollector delegate;
    private final OutputCollectorStreamlineEventLogger outputCollectorEventLogger;

    public EventLoggingSpoutOutputCollector(TopologyContext topologyContext, SpoutOutputCollector delegate,
                                            TestRunEventLogger eventLogger) {
        // we simply ignore the _delegate in SpoutOutputCollector and override all of the methods
        // this will work with subclass of SpoutOutputCollector since we only expose methods what we know about
        super(null);
        this.delegate = delegate;
        this.outputCollectorEventLogger = new OutputCollectorStreamlineEventLogger(topologyContext, eventLogger);
    }

    @Override
    public List<Integer> emit(String streamId, List<Object> tuple) {
        return outputCollectorEventLogger.emitWithLoggingEvent(streamId, tuple, t -> delegate.emit(streamId, t));
    }

    @Override
    public List<Integer> emit(List<Object> tuple) {
        return outputCollectorEventLogger.emitWithLoggingEvent(Utils.DEFAULT_STREAM_ID, tuple, t -> delegate.emit(t));
    }

    @Override
    public List<Integer> emit(String streamId, List<Object> tuple, Object messageId) {
        return outputCollectorEventLogger.emitWithLoggingEvent(streamId, tuple, t -> delegate.emit(streamId, t, messageId));
    }

    @Override
    public List<Integer> emit(List<Object> tuple, Object messageId) {
        return outputCollectorEventLogger.emitWithLoggingEvent(Utils.DEFAULT_STREAM_ID, tuple, t -> delegate.emit(t, messageId));
    }

    @Override
    public void emitDirect(int taskId, String streamId, List<Object> tuple) {
        outputCollectorEventLogger.emitDirectWithLoggingEvent(taskId, streamId, tuple, t -> delegate.emitDirect(taskId, streamId, t));
    }

    @Override
    public void emitDirect(int taskId, List<Object> tuple) {
        outputCollectorEventLogger.emitDirectWithLoggingEvent(taskId, Utils.DEFAULT_STREAM_ID, tuple, t -> delegate.emitDirect(taskId, t));
    }

    @Override
    public void emitDirect(int taskId, String streamId, List<Object> tuple, Object messageId) {
        outputCollectorEventLogger.emitDirectWithLoggingEvent(taskId, streamId, tuple, t -> delegate.emitDirect(taskId, streamId, t, messageId));
    }

    @Override
    public void emitDirect(int taskId, List<Object> tuple, Object messageId) {
        outputCollectorEventLogger.emitDirectWithLoggingEvent(taskId, Utils.DEFAULT_STREAM_ID, tuple, t -> delegate.emitDirect(taskId, t, messageId));
    }

    @Override
    public void reportError(Throwable throwable) {
        delegate.reportError(throwable);
    }

    @Override
    public long getPendingCount() {
        return delegate.getPendingCount();
    }
}