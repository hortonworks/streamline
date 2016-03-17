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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the topology DAG with edges between sources, processors and sinks.
 * There could be only one edge from a source to sink and the edge encapsulates
 * the different stream-groupings.
 */
public class TopologyDag implements Serializable {
    private Set<Source> sources = new HashSet<>();
    private Set<Sink> sinks = new HashSet<>();
    private Set<Processor> processors = new HashSet<>();

    private Map<Source, List<Edge>> dag = new HashMap<>();

    public TopologyDag add(Source source) {
        sources.add(source);
        return this;
    }

    public Set<Source> getSources() {
        return sources;
    }

    public TopologyDag add(Sink sink) {
        sinks.add(sink);
        return this;
    }

    public Set<Sink> getSinks() {
        return sinks;
    }

    public TopologyDag add(Processor processor) {
        sources.add(processor);
        sinks.add(processor);
        processors.add(processor);
        return this;
    }

    public Set<Processor> getProcessors() {
        return processors;
    }

    // single stream, shuffle grouping
    public void addEdge(Source source, Sink sink) {
        addEdge(source, sink, getDefaultStreamId(source));
    }

    // specify stream, shuffle grouping
    public void addEdge(Source source, Sink sink, String streamId) {
        addEdge(source, sink, streamId, Stream.Grouping.GROUPING_SHUFFLE);
    }

    // specify stream and grouping
    public void addEdge(Source source, Sink sink, String streamId, Stream.Grouping grouping) {
        ensureValid(source, sink);
        doAddEdge(source, sink, streamId, grouping);
    }

    public void removeEdge(Source source, Sink sink) {
        ensureValid(source, sink);
        Iterator<Edge> it = dag.get(source).iterator();
        while (it.hasNext()) {
            if (it.next().getSink().equals(sink)) {
                it.remove();
            }
        }
    }

    public void removeEdge(Source source, Sink sink, String streamId, Stream.Grouping grouping) {
        Iterator<Edge> it = dag.get(source).iterator();
        while (it.hasNext()) {
            Edge e = it.next();
            if (e.getSink().equals(sink)) {
                e.removeStreamGrouping(new StreamGrouping(source.getStream(streamId), grouping));
                if(e.getStreamGroupings().isEmpty()) {
                    it.remove();
                }
                break;
            }
        }
    }

    public List<Edge> getEdgesFrom(Component source) {
        List<Edge> result = dag.get(source);
        return result == null ? Collections.EMPTY_LIST : Collections.unmodifiableList(result);
    }

    public List<Edge> getEdgesTo(Component sink) {
        List<Edge> result = new ArrayList<>();
        for (List<Edge> edges : dag.values()) {
            for (Edge edge : edges) {
                if (edge.getSink().equals(sink)) {
                    result.add(edge);
                }
            }
        }
        return result;
    }

    public List<Edge> getEdges(Component component) {
        List<Edge> result = new ArrayList<>();
        result.addAll(getEdgesFrom(component));
        result.addAll(getEdgesTo(component));
        return result;
    }

    public List<Edge> getAllEdges() {
        List<Edge> result = new ArrayList<>();
        for (List<Edge> edges : dag.values()) {
            result.addAll(edges);
        }
        return result;
    }

    private void ensureValid(Source source, Sink sink) {
        if (!sources.contains(source)) {
            throw new IllegalArgumentException("Invalid Source");
        } else if (!sinks.contains(sink)) {
            throw new IllegalArgumentException("Invalid Sink");
        }
    }

    private void doAddEdge(Source source, Sink sink, String streamId, Stream.Grouping grouping) {
        List<Edge> edges = dag.get(source);
        if (edges == null) {
            edges = new ArrayList<>();
            dag.put(source, edges);
        }
        Stream stream = source.getStream(streamId);
        StreamGrouping streamGrouping = new StreamGrouping(stream, grouping);
        // source is already connected to sink, just add the stream grouping
        for (Edge e : edges) {
            if (e.getSink().equals(sink)) {
                e.addStreamGrouping(streamGrouping);
                return;
            }
        }
        edges.add(new Edge(source, sink, streamGrouping));
    }

    private String getDefaultStreamId(Source source) {
        return source.getDeclaredOutputStreams().iterator().next().getId();
    }


    @Override
    public String toString() {
        return "TopologyDag{" +
                "dag=" + dag +
                '}';
    }
}
