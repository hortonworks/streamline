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
package com.hortonworks.streamline.streams.layout.component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An edge between components in an IoT topology that encapsulates the
 * source stream(s) from where data is received and the grouping.
 * Edges can be created only from an {@link OutputComponent} to an {@link InputComponent}.
 * {@link Processor} is both an {@code InputComponent} and {@code OutputComponent} so could be
 * at the start or the end of the edge.
 */
public class Edge implements TopologyDagComponent {
    private String id;
    private OutputComponent from;
    private InputComponent to;
    private final Set<StreamGrouping> streamGroupings;

    public Edge() {
        this(null, null, null, Collections.<StreamGrouping>emptySet());
    }

    public Edge(String id, OutputComponent from, InputComponent to, String streamId, Stream.Grouping grouping) {
        this(id, from, to, new StreamGrouping(from.getOutputStream(streamId), grouping));
    }

    public Edge(String id, OutputComponent from, InputComponent to, StreamGrouping streamGrouping) {
        this(id, from, to, Collections.singleton(streamGrouping));
    }

    public Edge(String id, OutputComponent from, InputComponent to, Set<StreamGrouping> streamGroupings) {
        this.id = id;
        this.from = from;
        this.to = to;
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

    public void setFrom(OutputComponent from) {
        this.from = from;
    }

    public void setTo(InputComponent to) {
        this.to = to;
    }

    public OutputComponent getFrom() {
        return from;
    }

    public InputComponent getTo() {
        return to;
    }

    public Set<StreamGrouping> getStreamGroupings() {
        return Collections.unmodifiableSet(streamGroupings);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Edge edge = (Edge) o;

        if (from != null ? !from.equals(edge.from) : edge.from != null) return false;
        if (to != null ? !to.equals(edge.to) : edge.to != null) return false;
        return streamGroupings != null ? streamGroupings.equals(edge.streamGroupings) : edge.streamGroupings == null;

    }

    @Override
    public int hashCode() {
        int result = from != null ? from.hashCode() : 0;
        result = 31 * result + (to != null ? to.hashCode() : 0);
        result = 31 * result + (streamGroupings != null ? streamGroupings.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Edge{" +
                "from=" + from +
                ", to=" + to +
                ", streamGroupings=" + streamGroupings +
                '}';
    }

    @Override
    public void accept(TopologyDagVisitor visitor) {
        visitor.visit(this);
    }
}
