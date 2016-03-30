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
 * Represents the topology DAG with edges between input and output components.
 * There could be only one edge between components and the edge encapsulates
 * the different stream-groupings.
 */
public class TopologyDag implements Serializable {
    private Set<OutputComponent> outputComponents = new HashSet<>();
    private Set<InputComponent> inputComponents = new HashSet<>();

    private Map<OutputComponent, List<Edge>> dag = new HashMap<>();

    public TopologyDag add(OutputComponent component) {
        outputComponents.add(component);
        return this;
    }

    public Set<OutputComponent> getOutputComponents() {
        return outputComponents;
    }

    public TopologyDag add(InputComponent component) {
        inputComponents.add(component);
        return this;
    }

    public Set<InputComponent> getInputComponents() {
        return inputComponents;
    }

    public TopologyDag add(Processor processor) {
        outputComponents.add(processor);
        inputComponents.add(processor);
        return this;
    }

    // single stream, shuffle grouping
    public void addEdge(OutputComponent from, InputComponent to) {
        addEdge(from, to, getDefaultStreamId(from));
    }

    // specify stream, shuffle grouping
    public void addEdge(OutputComponent from, InputComponent to, String streamId) {
        addEdge(from, to, streamId, Stream.Grouping.SHUFFLE);
    }

    // specify stream and grouping
    public void addEdge(OutputComponent from, InputComponent to, String streamId, Stream.Grouping grouping) {
        ensureValid(from, to);
        doAddEdge(from, to, streamId, grouping);
    }

    public void removeEdge(OutputComponent from, InputComponent to) {
        ensureValid(from, to);
        Iterator<Edge> it = dag.get(from).iterator();
        while (it.hasNext()) {
            if (it.next().getTo().equals(to)) {
                it.remove();
            }
        }
    }

    public void removeEdge(OutputComponent from, InputComponent to, String streamId, Stream.Grouping grouping) {
        Iterator<Edge> it = dag.get(from).iterator();
        while (it.hasNext()) {
            Edge e = it.next();
            if (e.getTo().equals(to)) {
                e.removeStreamGrouping(new StreamGrouping(from.getOutputStream(streamId), grouping));
                if(e.getStreamGroupings().isEmpty()) {
                    it.remove();
                }
                break;
            }
        }
    }

    public List<Edge> getEdgesFrom(Component component) {
        List<Edge> result = dag.get(component);
        return result == null ? Collections.EMPTY_LIST : Collections.unmodifiableList(result);
    }

    public List<Edge> getEdgesTo(Component component) {
        List<Edge> result = new ArrayList<>();
        for (List<Edge> edges : dag.values()) {
            for (Edge edge : edges) {
                if (edge.getTo().equals(component)) {
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

    private void ensureValid(OutputComponent from, InputComponent to) {
        if (!outputComponents.contains(from)) {
            throw new IllegalArgumentException("Invalid from");
        } else if (!inputComponents.contains(to)) {
            throw new IllegalArgumentException("Invalid to");
        }
    }

    private void doAddEdge(OutputComponent from, InputComponent to, String streamId, Stream.Grouping grouping) {
        List<Edge> edges = dag.get(from);
        if (edges == null) {
            edges = new ArrayList<>();
            dag.put(from, edges);
        }
        Stream stream = from.getOutputStream(streamId);
        StreamGrouping streamGrouping = new StreamGrouping(stream, grouping);
        // output component is already connected to input component, just add the stream grouping
        for (Edge e : edges) {
            if (e.getTo().equals(to)) {
                e.addStreamGrouping(streamGrouping);
                return;
            }
        }
        edges.add(new Edge(from, to, streamGrouping));
    }

    private String getDefaultStreamId(OutputComponent source) {
        return source.getOutputStreams().iterator().next().getId();
    }

    @Override
    public String toString() {
        return "TopologyDag{" +
                "dag=" + dag +
                '}';
    }
}
