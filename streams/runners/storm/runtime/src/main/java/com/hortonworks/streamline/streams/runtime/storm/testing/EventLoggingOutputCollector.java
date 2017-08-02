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

import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.utils.Utils;

import java.util.Collection;
import java.util.List;

public class EventLoggingOutputCollector extends OutputCollector {
    private final OutputCollector delegate;
    private final OutputCollectorStreamlineEventLogger outputCollectorEventLogger;

    public EventLoggingOutputCollector(OutputCollector delegate, String componentName, TestRunEventLogger eventLogger) {
        // we simply ignore the _delegate in OutputCollector and override all of the methods
        // this will work with subclass of OutputCollector since we only expose methods what we know about
        super(null);
        this.delegate = delegate;
        this.outputCollectorEventLogger = new OutputCollectorStreamlineEventLogger(eventLogger, componentName);
    }

    @Override
    public List<Integer> emit(String streamId, Tuple anchor, List<Object> tuple) {
        return outputCollectorEventLogger.emitWithLoggingEvent(streamId, tuple, t -> delegate.emit(streamId, anchor, t));
    }

    @Override
    public List<Integer> emit(String streamId, List<Object> tuple) {
        return outputCollectorEventLogger.emitWithLoggingEvent(streamId, tuple, t -> delegate.emit(streamId, t));
    }

    @Override
    public List<Integer> emit(Collection<Tuple> anchors, List<Object> tuple) {
        return outputCollectorEventLogger.emitWithLoggingEvent(Utils.DEFAULT_STREAM_ID, tuple, t -> delegate.emit(anchors, t));
    }

    @Override
    public List<Integer> emit(Tuple anchor, List<Object> tuple) {
        return outputCollectorEventLogger.emitWithLoggingEvent(Utils.DEFAULT_STREAM_ID, tuple, t -> delegate.emit(anchor, t));
    }

    @Override
    public List<Integer> emit(List<Object> tuple) {
        return outputCollectorEventLogger.emitWithLoggingEvent(Utils.DEFAULT_STREAM_ID, tuple, t -> delegate.emit(t));
    }

    @Override
    public List<Integer> emit(String streamId, Collection<Tuple> anchors, List<Object> tuple) {
        return outputCollectorEventLogger.emitWithLoggingEvent(streamId, tuple, t -> delegate.emit(streamId, anchors, t));
    }

    @Override
    public void emitDirect(int taskId, String streamId, Tuple anchor, List<Object> tuple) {
        outputCollectorEventLogger.emitDirectWithLoggingEvent(streamId, tuple, t -> delegate.emitDirect(taskId, streamId, anchor, t));
    }

    @Override
    public void emitDirect(int taskId, String streamId, List<Object> tuple) {
        outputCollectorEventLogger.emitDirectWithLoggingEvent(streamId, tuple, t -> delegate.emitDirect(taskId, streamId, t));
    }

    @Override
    public void emitDirect(int taskId, Collection<Tuple> anchors, List<Object> tuple) {
        outputCollectorEventLogger.emitDirectWithLoggingEvent(Utils.DEFAULT_STREAM_ID, tuple, t -> delegate.emitDirect(taskId, anchors, t));
    }

    @Override
    public void emitDirect(int taskId, Tuple anchor, List<Object> tuple) {
        outputCollectorEventLogger.emitDirectWithLoggingEvent(Utils.DEFAULT_STREAM_ID, tuple, t -> delegate.emitDirect(taskId, anchor, t));
    }

    @Override
    public void emitDirect(int taskId, List<Object> tuple) {
        outputCollectorEventLogger.emitDirectWithLoggingEvent(Utils.DEFAULT_STREAM_ID, tuple, t -> delegate.emitDirect(taskId, t));
    }

    @Override
    public void emitDirect(int taskId, String streamId, Collection<Tuple> anchors, List<Object> tuple) {
        outputCollectorEventLogger.emitDirectWithLoggingEvent(streamId, tuple, t -> delegate.emitDirect(taskId, streamId, anchors, t));
    }

    @Override
    public void ack(Tuple tuple) {
        delegate.ack(tuple);
    }

    @Override
    public void fail(Tuple tuple) {
        delegate.fail(tuple);
    }

    @Override
    public void resetTimeout(Tuple tuple) {
        delegate.resetTimeout(tuple);
    }

    @Override
    public void reportError(Throwable throwable) {
        delegate.reportError(throwable);
    }
}