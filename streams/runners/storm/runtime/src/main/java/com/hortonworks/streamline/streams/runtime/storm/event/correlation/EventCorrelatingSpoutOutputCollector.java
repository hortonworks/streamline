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
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Values;

import java.util.Collections;
import java.util.List;

public class EventCorrelatingSpoutOutputCollector extends SpoutOutputCollector {
    private final TopologyContext topologyContext;
    private final SpoutOutputCollector delegate;
    private final StormEventCorrelationInjector eventCorrelationInjector;

    public EventCorrelatingSpoutOutputCollector(TopologyContext topologyContext, SpoutOutputCollector delegate) {
        // we simply ignore the _delegate in OutputCollector and override all of the methods
        // this will work with subclass of OutputCollector since we only expose methods what we know about
        super(null);
        this.topologyContext = topologyContext;
        this.delegate = delegate;
        this.eventCorrelationInjector = new StormEventCorrelationInjector();
    }


    @Override
    public List<Integer> emit(String streamId, List<Object> tuple) {
        StreamlineEvent newEvent = injectCorrelationInformation(tuple);
        return delegate.emit(streamId, new Values(newEvent));
    }

    @Override
    public List<Integer> emit(List<Object> tuple) {
        StreamlineEvent newEvent = injectCorrelationInformation(tuple);
        return delegate.emit(new Values(newEvent));
    }

    @Override
    public List<Integer> emit(String streamId, List<Object> tuple, Object messageId) {
        StreamlineEvent newEvent = injectCorrelationInformation(tuple);
        return delegate.emit(streamId, new Values(newEvent), messageId);
    }

    @Override
    public List<Integer> emit(List<Object> tuple, Object messageId) {
        StreamlineEvent newEvent = injectCorrelationInformation(tuple);
        return delegate.emit(new Values(newEvent), messageId);
    }

    @Override
    public void emitDirect(int taskId, String streamId, List<Object> tuple) {
        StreamlineEvent newEvent = injectCorrelationInformation(tuple);
        delegate.emitDirect(taskId, streamId, new Values(newEvent));
    }

    @Override
    public void emitDirect(int taskId, List<Object> tuple) {
        StreamlineEvent newEvent = injectCorrelationInformation(tuple);
        delegate.emitDirect(taskId, new Values(newEvent));
    }

    @Override
    public void emitDirect(int taskId, String streamId, List<Object> tuple, Object messageId) {
        StreamlineEvent newEvent = injectCorrelationInformation(tuple);
        delegate.emitDirect(taskId, streamId, new Values(newEvent), messageId);
    }

    @Override
    public void emitDirect(int taskId, List<Object> tuple, Object messageId) {
        StreamlineEvent newEvent = injectCorrelationInformation(tuple);
        delegate.emitDirect(taskId, new Values(newEvent), messageId);
    }

    @Override
    public void reportError(Throwable throwable) {
        delegate.reportError(throwable);
    }

    @Override
    public long getPendingCount() {
        return delegate.getPendingCount();
    }

    private StreamlineEvent injectCorrelationInformation(List<Object> tuple) {
        return eventCorrelationInjector.injectCorrelationInformation(tuple, Collections.emptyList(),
                topologyContext.getThisComponentId());
    }
}
