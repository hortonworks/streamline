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
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class EventCorrelatingOutputCollector extends OutputCollector {
    private final TopologyContext topologyContext;
    private final OutputCollector delegate;
    private final StormEventCorrelationInjector eventCorrelationInjector;

    public EventCorrelatingOutputCollector(TopologyContext topologyContext, OutputCollector delegate) {
        // we simply ignore the _delegate in OutputCollector and override all of the methods
        // this will work with subclass of OutputCollector since we only expose methods what we know about
        super(null);
        this.topologyContext = topologyContext;
        this.delegate = delegate;
        this.eventCorrelationInjector = new StormEventCorrelationInjector();
    }

    @Override
    public List<Integer> emit(String streamId, Tuple anchor, List<Object> tuple) {
        StreamlineEvent newEvent = injectCorrelationInformation(anchor, tuple);
        return delegate.emit(streamId, anchor, new Values(newEvent));
    }

    @Override
    public List<Integer> emit(Tuple anchor, List<Object> tuple) {
        StreamlineEvent newEvent = injectCorrelationInformation(anchor, tuple);
        return delegate.emit(anchor, new Values(newEvent));
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
    public List<Integer> emit(Collection<Tuple> anchors, List<Object> tuple) {
        StreamlineEvent newEvent = injectCorrelationInformation(anchors, tuple);
        return delegate.emit(anchors, new Values(newEvent));
    }

    @Override
    public List<Integer> emit(String streamId, Collection<Tuple> anchors, List<Object> tuple) {
        StreamlineEvent newEvent = injectCorrelationInformation(anchors, tuple);
        return delegate.emit(streamId, anchors, new Values(newEvent));
    }

    @Override
    public void emitDirect(int taskId, String streamId, Tuple anchor, List<Object> tuple) {
        StreamlineEvent newEvent = injectCorrelationInformation(anchor, tuple);
        delegate.emitDirect(taskId, streamId, anchor, new Values(newEvent));
    }

    @Override
    public void emitDirect(int taskId, String streamId, List<Object> tuple) {
        StreamlineEvent newEvent = injectCorrelationInformation(tuple);
        delegate.emitDirect(taskId, streamId, new Values(newEvent));
    }

    @Override
    public void emitDirect(int taskId, Collection<Tuple> anchors, List<Object> tuple) {
        StreamlineEvent newEvent = injectCorrelationInformation(anchors, tuple);
        delegate.emitDirect(taskId, anchors, new Values(newEvent));
    }

    @Override
    public void emitDirect(int taskId, Tuple anchor, List<Object> tuple) {
        StreamlineEvent newEvent = injectCorrelationInformation(anchor, tuple);
        delegate.emitDirect(taskId, anchor, new Values(newEvent));
    }

    @Override
    public void emitDirect(int taskId, List<Object> tuple) {
        StreamlineEvent newEvent = injectCorrelationInformation(tuple);
        delegate.emitDirect(taskId, new Values(newEvent));
    }

    @Override
    public void emitDirect(int taskId, String streamId, Collection<Tuple> anchors, List<Object> tuple) {
        StreamlineEvent newEvent = injectCorrelationInformation(anchors, tuple);
        delegate.emitDirect(taskId, streamId, anchors, new Values(newEvent));
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

    private StreamlineEvent injectCorrelationInformation(Tuple anchor, List<Object> tuple) {
        return eventCorrelationInjector.injectCorrelationInformation(tuple,
                Collections.singletonList(anchor), topologyContext.getThisComponentId());
    }

    private StreamlineEvent injectCorrelationInformation(List<Object> tuple) {
        return eventCorrelationInjector.injectCorrelationInformation(tuple,
                Collections.emptyList(), topologyContext.getThisComponentId());
    }

    private StreamlineEvent injectCorrelationInformation(Collection<Tuple> anchors, List<Object> tuple) {
        return eventCorrelationInjector.injectCorrelationInformation(tuple,
                new ArrayList<>(anchors), topologyContext.getThisComponentId());
    }

}
