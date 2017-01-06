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

import com.google.common.collect.Sets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Represents the topology DAG with edges between input and output components.
 * There could be only one edge between components and the edge encapsulates
 * the different stream-groupings.
 */
public class TopologyDag implements Serializable {
    private final Set<OutputComponent> outputComponents = new LinkedHashSet<>();
    private final Set<InputComponent> inputComponents = new LinkedHashSet<>();

    private final Map<OutputComponent, List<Edge>> dag = new LinkedHashMap<>();

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

    public Set<Component> getComponents() {
        return Sets.union(inputComponents, outputComponents);
    }

    // single stream, shuffle grouping
    public void addEdge(OutputComponent from, InputComponent to) {
        addEdge(UUID.randomUUID().toString(), from, to, getDefaultStreamId(from));
    }

    // specify stream, shuffle grouping
    public void addEdge(String id, OutputComponent from, InputComponent to, String streamId) {
        addEdge(id, from, to, streamId, Stream.Grouping.SHUFFLE);
    }

    // specify stream and grouping
    public void addEdge(String id, OutputComponent from, InputComponent to, String streamId, Stream.Grouping grouping) {
        addEdge(id, from, to, new StreamGrouping(from.getOutputStream(streamId), grouping));
    }

    public void addEdge(Edge edge) {
        for (StreamGrouping streamGrouping : edge.getStreamGroupings()) {
            addEdge(edge.getId(), edge.getFrom(), edge.getTo(), streamGrouping);
        }
    }

    // specify stream grouping
    public void addEdge(String id, OutputComponent from, InputComponent to, StreamGrouping streamGrouping) {
        ensureValid(from, to);
        doAddEdge(id, from, to, streamGrouping);
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

    /**
     * Returns all the components adjacent to the given component.
     *
     * @param component the component
     * @return List of all adjacent components
     */
    public List<Component> adjacent(Component component) {
        List<Component> adj = new ArrayList<>();
        List<Edge> edges = dag.get(component);
        if (edges != null) {
            for(Edge edge: edges) {
                adj.add(edge.getTo());
            }
        }
        return adj;
    }

    /**
     * Visits the topology dag and the edges in topological order
     *
     * @param visitor topology dag visitor
     */
    public void traverse(TopologyDagVisitor visitor) {
        for(Component component: topOrder()) {
            component.accept(visitor);
            for (Edge edge : getEdgesFrom(component)) {
                edge.accept(visitor);
            }
        }
    }

    private enum  State {
        VISITED, VISITING
    }

    // package access for testing.
    List<Component> topOrder() {
        Map<Component, State> state = new HashMap<>();
        List<Component> res = new ArrayList<>();
        for(Component component: Sets.union(inputComponents, outputComponents)) {
            if (state.get(component) != State.VISITED) {
                res.addAll(dfs(component, state));
            }
        }
        Collections.reverse(res);
        return res;
    }

    private List<Component> dfs(Component current, Map<Component, State> state) {
        List<Component> res = new ArrayList<>();
        if (state.get(current) == State.VISITING) {
            throw new IllegalStateException("Cycle");
        }
        state.put(current, State.VISITING);
        for (Component adj : adjacent(current)) {
            if (state.get(adj) != State.VISITED) {
                res.addAll(dfs(adj, state));
            }
        }
        state.put(current, State.VISITED);
        res.add(current);
        return res;
    }

    private void ensureValid(OutputComponent from, InputComponent to) {
        if (!outputComponents.contains(from)) {
            throw new IllegalArgumentException("Invalid from");
        } else if (!inputComponents.contains(to)) {
            throw new IllegalArgumentException("Invalid to");
        }
    }

    private void doAddEdge(String id, OutputComponent from, InputComponent to, StreamGrouping streamGrouping) {
        List<Edge> edges = dag.get(from);
        if (edges == null) {
            edges = new ArrayList<>();
            dag.put(from, edges);
        }
        // output component is already connected to input component, just add the stream grouping
        for (Edge e : edges) {
            if (e.getTo().equals(to)) {
                e.addStreamGrouping(streamGrouping);
                return;
            }
        }
        edges.add(new Edge(id, from, to, streamGrouping));
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
