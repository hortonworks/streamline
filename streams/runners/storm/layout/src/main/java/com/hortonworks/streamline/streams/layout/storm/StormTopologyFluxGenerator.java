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
package com.hortonworks.streamline.streams.layout.storm;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;
import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.layout.component.Component;
import com.hortonworks.streamline.streams.layout.component.Edge;
import com.hortonworks.streamline.streams.layout.component.InputComponent;
import com.hortonworks.streamline.streams.layout.component.Stream;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import com.hortonworks.streamline.streams.layout.component.OutputComponent;
import com.hortonworks.streamline.streams.layout.component.StreamGrouping;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.layout.component.rule.Rule;
import com.hortonworks.streamline.streams.layout.component.rule.expression.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.hortonworks.streamline.streams.layout.storm.StormTopologyLayoutConstants.YAML_KEY_ID;
import static com.hortonworks.streamline.streams.layout.storm.StormTopologyLayoutConstants.YAML_KEY_STREAMS;
import static com.hortonworks.streamline.streams.layout.storm.StormTopologyLayoutConstants.YAML_KEY_TO;

public class StormTopologyFluxGenerator extends TopologyDagVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(StormTopologyFluxGenerator.class);

    private static final int DELTA = 5;

    private final FluxComponentFactory fluxComponentFactory;

    private final List<Map.Entry<String, Map<String, Object>>> keysAndComponents = new ArrayList<>();
    private final TopologyDag topologyDag;
    private final Map<String, Object> config;
    private final Config topologyConfig;

    public StormTopologyFluxGenerator(TopologyLayout topologyLayout, Map<String, Object> config, Path extraJarsLocation) {
        this.topologyDag = topologyLayout.getTopologyDag();
        this.topologyConfig = topologyLayout.getConfig();
        this.config = config;
        fluxComponentFactory = new FluxComponentFactory(extraJarsLocation);
    }

    @Override
    public void visit(StreamlineSource source) {
        keysAndComponents.add(makeEntry(StormTopologyLayoutConstants.YAML_KEY_SPOUTS,
                getYamlComponents(fluxComponentFactory.getFluxComponent(source), source)));
    }

    @Override
    public void visit(StreamlineSink sink) {
        keysAndComponents.add(makeEntry(StormTopologyLayoutConstants.YAML_KEY_BOLTS,
                getYamlComponents(fluxComponentFactory.getFluxComponent(sink), sink)));
    }

    @Override
    public void visit(StreamlineProcessor processor) {
        keysAndComponents.add(makeEntry(StormTopologyLayoutConstants.YAML_KEY_BOLTS,
                getYamlComponents(fluxComponentFactory.getFluxComponent(processor), processor)));
    }


    @Override
    public void visit(final RulesProcessor rulesProcessor) {
        rulesProcessor.getConfig().setAny("outputStreams", rulesProcessor.getOutputStreams());
        List<Rule> rulesWithWindow = new ArrayList<>();
        List<Rule> rulesWithoutWindow = new ArrayList<>();
        for (Rule rule : rulesProcessor.getRules()) {
            if (rule.getWindow() != null) {
                rulesWithWindow.add(rule);
            } else {
                rulesWithoutWindow.add(rule);
            }
        }
        // handle windowed rules with WindowRuleBoltFluxComponent
        if (!rulesWithWindow.isEmpty()) {
            Multimap<Window, Rule> windowedRules = ArrayListMultimap.create();
            for (Rule rule : rulesWithWindow) {
                windowedRules.put(rule.getWindow(), rule);
            }
            int windowedRulesProcessorId = 0;
            // create windowed bolt per unique window configuration
            for (Map.Entry<Window, Collection<Rule>> entry : windowedRules.asMap().entrySet()) {
                RulesProcessor windowedRulesProcessor = new RulesProcessor(rulesProcessor);
                windowedRulesProcessor.setRules(new ArrayList<>(entry.getValue()));
                windowedRulesProcessor.setId(rulesProcessor.getId() + "." + ++windowedRulesProcessorId);
                windowedRulesProcessor.setName("WindowedRulesProcessor");
                windowedRulesProcessor.getConfig().setAny(RulesProcessor.CONFIG_KEY_RULES, Collections2.transform(entry.getValue(), new Function<Rule, Long>() {
                    @Override
                    public Long apply(Rule input) {
                        return input.getId();
                    }
                }));
                LOG.debug("Rules processor with window {}", windowedRulesProcessor);
                keysAndComponents.add(makeEntry(StormTopologyLayoutConstants.YAML_KEY_BOLTS,
                        getYamlComponents(fluxComponentFactory.getFluxComponent(windowedRulesProcessor), windowedRulesProcessor)));
                // Wire the windowed bolt with the appropriate edges
                wireWindowedRulesProcessor(windowedRulesProcessor, topologyDag.getEdgesTo(rulesProcessor),
                        topologyDag.getEdgesFrom(rulesProcessor));
                mayBeUpdateTopologyConfig(entry.getKey());
            }
        }
        if (rulesWithoutWindow.isEmpty()) {
            removeFluxStreamsTo(getFluxId(rulesProcessor));
        } else {
            rulesProcessor.setRules(rulesWithoutWindow);
            rulesProcessor.getConfig().setAny(RulesProcessor.CONFIG_KEY_RULES, Collections2.transform(rulesWithoutWindow, new Function<Rule, Long>() {
                @Override
                public Long apply(Rule input) {
                    return input.getId();
                }
            }));
            keysAndComponents.add(makeEntry(StormTopologyLayoutConstants.YAML_KEY_BOLTS,
                    getYamlComponents(fluxComponentFactory.getFluxComponent(rulesProcessor), rulesProcessor)));
        }
    }

    private void mayBeUpdateTopologyConfig(Window window) {
        int messageTimeoutSecs = DELTA;
        int maxPending = DELTA;
        if (window.getWindowLength() instanceof Window.Duration) {
            messageTimeoutSecs += ((Window.Duration) window.getWindowLength()).getDurationMs()/1000;
        } else if (window.getWindowLength() instanceof Window.Count) {
            maxPending += ((Window.Count) window.getWindowLength()).getCount();
        }
        if (window.getSlidingInterval() instanceof Window.Duration) {
            messageTimeoutSecs += ((Window.Duration) window.getSlidingInterval()).getDurationMs()/1000;
        } else if (window.getSlidingInterval() instanceof Window.Count) {
            maxPending += ((Window.Count) window.getSlidingInterval()).getCount();
        }
        setIfGreater(StormTopologyLayoutConstants.TOPOLOGY_MESSAGE_TIMEOUT_SECS, messageTimeoutSecs);
        setIfGreater(StormTopologyLayoutConstants.TOPOLOGY_MAX_SPOUT_PENDING, maxPending);
    }

    private void setIfGreater(String key, int value) {
        Integer curVal = topologyConfig.getInt(key, DELTA);
        if (value > curVal) {
            topologyConfig.setAny(key, value);
        }
    }

    private void removeFluxStreamsTo(String componentId) {
        Iterator<Map.Entry<String, Map<String, Object>>> it = keysAndComponents.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Map<String, Object>> entry = it.next();
            if (entry.getKey().equals(YAML_KEY_STREAMS)
                    && entry.getValue().get(YAML_KEY_TO).equals(componentId)) {
                LOG.debug("Removing entry {} from yaml keys and components", entry);
                it.remove();
            }
        }
    }

    private void wireWindowedRulesProcessor(RulesProcessor windowedRulesProcessor, List<Edge> inEdges, List<Edge> outEdges) {
        for (Edge edge : inEdges) {
            for (StreamGrouping streamGrouping : edge.getStreamGroupings()) {
                addEdge(edge.getFrom(),
                        windowedRulesProcessor,
                        streamGrouping.getStream().getId(),
                        streamGrouping.getGrouping(),
                        streamGrouping.getFields());
            }
        }

        for (Edge edge : outEdges) {
            for (StreamGrouping streamGrouping : edge.getStreamGroupings()) {
                addEdge(windowedRulesProcessor,
                        edge.getTo(),
                        streamGrouping.getStream().getId(),
                        streamGrouping.getGrouping(),
                        streamGrouping.getFields());
            }
        }
    }

    @Override
    public void visit(Edge edge) {
        if (sourceYamlComponentExists(edge)) {
            for (StreamGrouping streamGrouping : edge.getStreamGroupings()) {
                addEdge(edge.getFrom(),
                        edge.getTo(),
                        streamGrouping.getStream().getId(),
                        streamGrouping.getGrouping(),
                        streamGrouping.getFields());
            }
        }
    }

    private boolean sourceYamlComponentExists(Edge edge) {
        for (Map.Entry<String, Map<String, Object>> entry : keysAndComponents) {
            String id = (String) entry.getValue().get(YAML_KEY_ID);
            if (getFluxId(edge.getFrom()).equals(id)) {
                return true;
            }
        }
        return false;
    }

    public List<Map.Entry<String, Map<String, Object>>> getYamlKeysAndComponents() {
        return keysAndComponents;
    }

    public Config getTopologyConfig() {
        return topologyConfig;
    }

    private Map<String, Object> getYamlComponents(FluxComponent fluxComponent, Component topologyComponent) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.putAll(config);
        props.putAll(topologyComponent.getConfig().getProperties());
        // below line is needed becuase kafka, normalization, notification and rules flux components need design time entities
        props.put(StormTopologyLayoutConstants.STREAMLINE_COMPONENT_CONF_KEY, topologyComponent);
        fluxComponent.withConfig(props);

        for (Map<String, Object> referencedComponent : fluxComponent.getReferencedComponents()) {
            keysAndComponents.add(makeEntry(StormTopologyLayoutConstants.YAML_KEY_COMPONENTS, referencedComponent));
        }

        Map<String, Object> yamlComponent = fluxComponent.getComponent();
        yamlComponent.put(StormTopologyLayoutConstants.YAML_KEY_ID, getFluxId(topologyComponent));
        return yamlComponent;
    }

    private void addEdge(OutputComponent from, InputComponent to, String streamId, Stream.Grouping groupingType, List<String> fields) {
        LinkFluxComponent fluxComponent = new LinkFluxComponent();
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> grouping = new LinkedHashMap<>();
        if (Stream.Grouping.FIELDS.equals(groupingType)) {
            grouping.put(StormTopologyLayoutConstants.YAML_KEY_TYPE, StormTopologyLayoutConstants.YAML_KEY_CUSTOM_GROUPING);
            Map<Object, Object> customGroupingClass = new HashMap<>();
            customGroupingClass.put(StormTopologyLayoutConstants.YAML_KEY_CLASS_NAME, StormTopologyLayoutConstants.YAML_KEY_CUSTOM_GROUPING_CLASSNAME);
            List<Object> constructorArgs = new ArrayList<>();
            constructorArgs.add(fields);
            customGroupingClass.put(StormTopologyLayoutConstants.YAML_KEY_CONSTRUCTOR_ARGS, constructorArgs);
            grouping.put(StormTopologyLayoutConstants.YAML_KEY_CUSTOM_GROUPING_CLASS, customGroupingClass);
        } else if (Stream.Grouping.SHUFFLE.equals(groupingType)) {
            grouping.put(StormTopologyLayoutConstants.YAML_KEY_TYPE, StormTopologyLayoutConstants.YAML_KEY_LOCAL_OR_SHUFFLE_GROUPING);
        } else {
            throw new RuntimeException("Unsupported grouping type: " + groupingType + "  for storm link ");
        }
        grouping.put(StormTopologyLayoutConstants.YAML_KEY_STREAM_ID, streamId);
        fluxComponent.updateLinkComponentWithGrouping(grouping);
        config.put(StormTopologyLayoutConstants.YAML_KEY_FROM, getFluxId(from));
        config.put(StormTopologyLayoutConstants.YAML_KEY_TO, getFluxId(to));
        fluxComponent.withConfig(config);
        Map<String, Object> yamlComponent = fluxComponent.getComponent();
        keysAndComponents.add(makeEntry(StormTopologyLayoutConstants.YAML_KEY_STREAMS, yamlComponent));
    }

    private Map.Entry<String, Map<String, Object>> makeEntry(String key, Map<String, Object> component) {
        return new AbstractMap.SimpleImmutableEntry<>(key, component);
    }

    private String getFluxId(Component component) {
        return component.getId() + "-" + component.getName();
    }
}
