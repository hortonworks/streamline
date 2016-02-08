/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hortonworks.iotas.layout.design.component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An edge between components in an IoT topology that encapsulates the
 * source stream(s) from where data is received and the grouping.
 * Edges can be created only between a {@link Source} and {@link Sink} component.
 * {@link Processor} inherits from both Source and Sink so could be
 * at the start or the end of the edge.
 */
public class Edge {
    private final Source source;
    private final Sink sink;
    private final Set<StreamGrouping> streamGroupings;

    public Edge(Source source, Sink sink, String streamId, Stream.Grouping grouping) {
        this(source, sink, new StreamGrouping(source.getStream(streamId), grouping));
    }

    public Edge(Source source, Sink sink, StreamGrouping streamGrouping) {
        this(source, sink, Collections.singleton(streamGrouping));
    }

    public Edge(Source source, Sink sink, Set<StreamGrouping> streamGroupings) {
        this.source = source;
        this.sink = sink;
        this.streamGroupings = new HashSet<>(streamGroupings);
    }

    public void addStreamGrouping(StreamGrouping streamGrouping) {
        this.streamGroupings.add(streamGrouping);
    }

    public void addStreamGroupings(Set<StreamGrouping> streamGroupings) {
        this.streamGroupings.addAll(streamGroupings);
    }

    public void removeStreamGrouping(StreamGrouping streamGrouping) {
        this.streamGroupings.remove(streamGrouping);
    }

    public void removeStreamGroupings(Set<StreamGrouping> streamGroupings) {
        this.streamGroupings.removeAll(streamGroupings);
    }

    public Source getSource() {
        return source;
    }

    public Sink getSink() {
        return sink;
    }

    public Set<StreamGrouping> getStreamGroupings() {
        return Collections.unmodifiableSet(streamGroupings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Edge edge = (Edge) o;

        if (source != null ? !source.equals(edge.source) : edge.source != null) return false;
        if (sink != null ? !sink.equals(edge.sink) : edge.sink != null) return false;
        return streamGroupings != null ? streamGroupings.equals(edge.streamGroupings) : edge.streamGroupings == null;

    }

    @Override
    public int hashCode() {
        int result = source != null ? source.hashCode() : 0;
        result = 31 * result + (sink != null ? sink.hashCode() : 0);
        result = 31 * result + (streamGroupings != null ? streamGroupings.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Edge{" +
                "source=" + source +
                ", sink=" + sink +
                ", streamGroupings=" + streamGroupings +
                '}';
    }
}
