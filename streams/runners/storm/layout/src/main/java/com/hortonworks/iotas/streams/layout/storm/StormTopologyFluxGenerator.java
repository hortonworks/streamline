package com.hortonworks.iotas.streams.layout.storm;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;
import com.hortonworks.iotas.streams.layout.component.Component;
import com.hortonworks.iotas.streams.layout.component.Edge;
import com.hortonworks.iotas.streams.layout.component.InputComponent;
import com.hortonworks.iotas.streams.layout.component.IotasProcessor;
import com.hortonworks.iotas.streams.layout.component.IotasSink;
import com.hortonworks.iotas.streams.layout.component.IotasSource;
import com.hortonworks.iotas.streams.layout.component.OutputComponent;
import com.hortonworks.iotas.streams.layout.component.StreamGrouping;
import com.hortonworks.iotas.streams.layout.component.TopologyDag;
import com.hortonworks.iotas.streams.layout.component.TopologyDagVisitor;
import com.hortonworks.iotas.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.iotas.streams.layout.component.rule.Rule;
import com.hortonworks.iotas.streams.layout.component.rule.expression.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.hortonworks.iotas.streams.layout.storm.StormTopologyLayoutConstants.YAML_KEY_ID;
import static com.hortonworks.iotas.streams.layout.storm.StormTopologyLayoutConstants.YAML_KEY_STREAMS;
import static com.hortonworks.iotas.streams.layout.storm.StormTopologyLayoutConstants.YAML_KEY_TO;

public class StormTopologyFluxGenerator extends TopologyDagVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(StormTopologyFluxGenerator.class);

    private final List<Map.Entry<String, Map<String, Object>>> keysAndComponents = new ArrayList<>();

    private TopologyDag topologyDag;

    private FluxComponentFactory fluxComponentFactory = new FluxComponentFactory();

    public StormTopologyFluxGenerator(TopologyDag topologyDag) {
        this.topologyDag = topologyDag;
    }

    @Override
    public void visit(IotasSource iotasSource) {
        keysAndComponents.add(makeEntry(StormTopologyLayoutConstants.YAML_KEY_SPOUTS,
                getYamlComponents(fluxComponentFactory.getFluxComponent(iotasSource), iotasSource)));
    }

    @Override
    public void visit(IotasSink iotasSink) {
        keysAndComponents.add(makeEntry(StormTopologyLayoutConstants.YAML_KEY_BOLTS,
                getYamlComponents(fluxComponentFactory.getFluxComponent(iotasSink), iotasSink)));
    }

    @Override
    public void visit(IotasProcessor iotasProcessor) {
        keysAndComponents.add(makeEntry(StormTopologyLayoutConstants.YAML_KEY_BOLTS,
                getYamlComponents(fluxComponentFactory.getFluxComponent(iotasProcessor), iotasProcessor)));
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
            for (Collection<Rule> rules : windowedRules.asMap().values()) {
                RulesProcessor windowedRulesProcessor = new RulesProcessor(rulesProcessor);
                windowedRulesProcessor.setRules(new ArrayList<>(rules));
                windowedRulesProcessor.setId(rulesProcessor.getId() + "." + ++windowedRulesProcessorId);
                windowedRulesProcessor.setName("WindowedRulesProcessor");
                windowedRulesProcessor.getConfig().setAny(RulesProcessor.CONFIG_KEY_RULES, Collections2.transform(rules, new Function<Rule, Long>() {
                    @Override
                    public Long apply(Rule input) {
                        return input.getId();
                    }
                }));
                LOG.debug("Rules processor with window {}", windowedRulesProcessor);
                keysAndComponents.add(makeEntry(StormTopologyLayoutConstants.YAML_KEY_BOLTS,
                        getYamlComponents(new WindowRuleBoltFluxComponent(windowedRulesProcessor), windowedRulesProcessor)));
                // Wire the windowed bolt with the appropriate edges
                wireWindowedRulesProcessor(windowedRulesProcessor, topologyDag.getEdgesTo(rulesProcessor),
                        topologyDag.getEdgesFrom(rulesProcessor));
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
                    getYamlComponents(new RuleBoltFluxComponent(rulesProcessor), rulesProcessor)));
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
                        String.valueOf(streamGrouping.getGrouping()),
                        streamGrouping.getFields());
            }
        }

        for (Edge edge : outEdges) {
            for (StreamGrouping streamGrouping : edge.getStreamGroupings()) {
                addEdge(windowedRulesProcessor,
                        edge.getTo(),
                        streamGrouping.getStream().getId(),
                        String.valueOf(streamGrouping.getGrouping()),
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
                        String.valueOf(streamGrouping.getGrouping()),
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

    private Map<String, Object> getYamlComponents(FluxComponent fluxComponent, Component topologyComponent) {
        fluxComponent.withConfig(topologyComponent.getConfig().getProperties());
        for (Map<String, Object> referencedComponent : fluxComponent.getReferencedComponents()) {
            keysAndComponents.add(makeEntry(StormTopologyLayoutConstants.YAML_KEY_COMPONENTS, referencedComponent));
        }
        Map<String, Object> yamlComponent = fluxComponent.getComponent();
        yamlComponent.put(YAML_KEY_ID, getFluxId(topologyComponent));
        return yamlComponent;
    }

    private void addEdge(OutputComponent from, InputComponent to, String streamId, String groupingType, List<String> fields) {
        LinkFluxComponent fluxComponent = new LinkFluxComponent();
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> grouping = new LinkedHashMap<>();
        grouping.put(StormTopologyLayoutConstants.YAML_KEY_TYPE, groupingType);
        grouping.put(StormTopologyLayoutConstants.YAML_KEY_STREAM_ID, streamId);
        if (fields != null) {
            grouping.put(StormTopologyLayoutConstants.YAML_KEY_ARGS, fields);
        }
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
