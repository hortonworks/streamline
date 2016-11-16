package org.apache.streamline.streams.catalog.topology.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.streamline.common.Config;
import org.apache.streamline.streams.catalog.RuleInfo;
import org.apache.streamline.streams.catalog.BranchRuleInfo;
import org.apache.streamline.streams.catalog.StreamInfo;
import org.apache.streamline.streams.catalog.TopologyComponent;
import org.apache.streamline.streams.catalog.TopologyEdge;
import org.apache.streamline.streams.catalog.TopologyOutputComponent;
import org.apache.streamline.streams.catalog.TopologyProcessor;
import org.apache.streamline.streams.catalog.TopologySink;
import org.apache.streamline.streams.catalog.TopologySource;
import org.apache.streamline.streams.catalog.WindowInfo;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.layout.component.Edge;
import org.apache.streamline.streams.layout.component.InputComponent;
import org.apache.streamline.streams.layout.component.StreamlineComponent;
import org.apache.streamline.streams.layout.component.StreamlineProcessor;
import org.apache.streamline.streams.layout.component.StreamlineSink;
import org.apache.streamline.streams.layout.component.StreamlineSource;
import org.apache.streamline.streams.layout.component.OutputComponent;
import org.apache.streamline.streams.layout.component.Stream;
import org.apache.streamline.streams.layout.component.StreamGrouping;
import org.apache.streamline.streams.layout.component.impl.KafkaSource;
import org.apache.streamline.streams.layout.component.impl.NotificationSink;
import org.apache.streamline.streams.layout.component.impl.RulesProcessor;
import org.apache.streamline.streams.layout.component.impl.normalization.NormalizationConfig;
import org.apache.streamline.streams.layout.component.impl.normalization.NormalizationProcessor;
import org.apache.streamline.streams.layout.component.impl.splitjoin.JoinAction;
import org.apache.streamline.streams.layout.component.impl.splitjoin.JoinProcessor;
import org.apache.streamline.streams.layout.component.impl.splitjoin.SplitAction;
import org.apache.streamline.streams.layout.component.impl.splitjoin.SplitProcessor;
import org.apache.streamline.streams.layout.component.impl.splitjoin.StageAction;
import org.apache.streamline.streams.layout.component.impl.splitjoin.StageProcessor;
import org.apache.streamline.streams.layout.component.rule.Rule;
import org.apache.streamline.streams.catalog.topology.TopologyComponentBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.streamline.common.ComponentTypes.JOIN;
import static org.apache.streamline.common.ComponentTypes.KAFKA;
import static org.apache.streamline.common.ComponentTypes.NORMALIZATION;
import static org.apache.streamline.common.ComponentTypes.NOTIFICATION;
import static org.apache.streamline.common.ComponentTypes.RULE;
import static org.apache.streamline.common.ComponentTypes.BRANCH;
import static org.apache.streamline.common.ComponentTypes.SPLIT;
import static org.apache.streamline.common.ComponentTypes.STAGE;
import static org.apache.streamline.common.ComponentTypes.WINDOW;

import static java.util.AbstractMap.SimpleImmutableEntry;

/**
 * Constructs various topology components based on the
 * TopologyComponent catalog entities
 */
public class TopologyComponentFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyComponentFactory.class);

    private final Map<Class<?>, Map<String, ?>> providerMap;
    private final StreamCatalogService catalogService;

    public TopologyComponentFactory(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
        ImmutableMap.Builder<Class<?>, Map<String, ?>> builder = ImmutableMap.builder();
        builder.put(StreamlineSource.class, createSourceProviders());
        builder.put(StreamlineProcessor.class, createProcessorProviders());
        builder.put(StreamlineSink.class, createSinkProviders());
        providerMap = builder.build();
    }

    public StreamlineSource getStreamlineSource(TopologySource topologySource) {
        StreamlineSource source = getStreamlineComponent(StreamlineSource.class, topologySource);
        source.addOutputStreams(createOutputStreams(topologySource));
        return source;
    }

    public StreamlineProcessor getStreamlineProcessor(TopologyProcessor topologyProcessor) {
        StreamlineProcessor processor = getStreamlineComponent(StreamlineProcessor.class, topologyProcessor);
        if (processor.getOutputStreams() == null || processor.getOutputStreams().isEmpty()) {
            processor.addOutputStreams(createOutputStreams(topologyProcessor));
        }
        return processor;
    }

    public StreamlineSink getStreamlineSink(TopologySink topologySink) {
        return getStreamlineComponent(StreamlineSink.class, topologySink);
    }

    public Edge getStreamlineEdge(TopologyEdge topologyEdge) {
        Edge edge = new Edge();
        edge.setFrom(getOutputComponent(topologyEdge));
        edge.setTo(getInputComponent(topologyEdge));
        Set<StreamGrouping> streamGroupings = new HashSet<>();
        for (TopologyEdge.StreamGrouping streamGrouping : topologyEdge.getStreamGroupings()) {
            Stream stream = getStream(catalogService.getStreamInfo(topologyEdge.getTopologyId(),
                    streamGrouping.getStreamId(), topologyEdge.getVersionId()));
            Stream.Grouping grouping = Stream.Grouping.valueOf(streamGrouping.getGrouping().name());
            streamGroupings.add(new StreamGrouping(stream, grouping, streamGrouping.getFields()));
        }
        edge.addStreamGroupings(streamGroupings);
        return edge;
    }

    private TopologyComponentBundle getTopologyComponentBundle (TopologyComponent topologyComponent) {
        TopologyComponentBundle topologyComponentBundle = catalogService.getTopologyComponentBundle(topologyComponent.getTopologyComponentBundleId());
        if (topologyComponentBundle == null) {
            String msg = "TopologyComponentBundle not found for topologyComponent " + topologyComponent;
            LOG.debug(msg);
            throw new RuntimeException(msg);
        }
        return topologyComponentBundle;
    }

    private <T extends StreamlineComponent> T getStreamlineComponent(Class<T> clazz,
                                                                     TopologyComponent topologyComponent) {
        TopologyComponentBundle topologyComponentBundle = getTopologyComponentBundle(topologyComponent);
        StreamlineComponent component = getProvider(clazz, topologyComponentBundle.getSubType())
                .create(topologyComponent);
        component.setId(topologyComponent.getId().toString());
        component.setName(topologyComponent.getName());
        component.setConfig(topologyComponent.getConfig());
        component.setTopologyComponentBundleId(topologyComponentBundle.getId().toString());
        component.setTransformationClass(topologyComponentBundle.getTransformationClass());
        return clazz.cast(component);
    }

    private OutputComponent getOutputComponent(TopologyEdge topologyEdge) {
        TopologySource topologySource;
        TopologyProcessor topologyProcessor;
        if ((topologySource = catalogService.getTopologySource(topologyEdge.getTopologyId(), topologyEdge.getFromId(),
                topologyEdge.getVersionId())) != null) {
            return getStreamlineSource(topologySource);
        } else if ((topologyProcessor = catalogService.getTopologyProcessor(topologyEdge.getTopologyId(), topologyEdge.getFromId(),
                topologyEdge.getVersionId())) != null) {
            return getStreamlineProcessor(topologyProcessor);
        } else {
            throw new IllegalArgumentException("Invalid from id for edge " + topologyEdge);
        }
    }

    private InputComponent getInputComponent(TopologyEdge topologyEdge) {
        TopologySink topologySink;
        TopologyProcessor topologyProcessor;
        if ((topologySink = catalogService.getTopologySink(topologyEdge.getTopologyId(), topologyEdge.getToId(),
                topologyEdge.getVersionId())) != null) {
            return getStreamlineSink(topologySink);
        } else if ((topologyProcessor = catalogService.getTopologyProcessor(topologyEdge.getTopologyId(), topologyEdge.getToId(),
                topologyEdge.getVersionId())) != null) {
            return getStreamlineProcessor(topologyProcessor);
        } else {
            throw new IllegalArgumentException("Invalid to id for edge " + topologyEdge);
        }
    }

    public Stream getStream(StreamInfo streamInfo) {
        return new Stream(streamInfo.getStreamId(), streamInfo.getFields());
    }

    /*
     * Its optional to register the specific child providers. Its needed only if
     * the specific child component exists and needs to be passed in the runtime.
     * In that case the specific child component would be added to the topology dag vertex.
     * Otherwise an instance of StreamlineSource is used.
     */
    private Map<String, Provider<StreamlineSource>> createSourceProviders() {
        ImmutableMap.Builder<String, Provider<StreamlineSource>> builder = ImmutableMap.builder();
        builder.put(kafkaSourceProvider());
        return builder.build();
    }

    /*
     * Its optional to register the specific child providers. Its needed only if
     * the specific child component exists and needs to be passed in the runtime.
     * In that case the specific child component would be added to the topology dag vertex.
     * Otherwise an instance of StreamlineProcessor is used.
     */
    private Map<String, Provider<StreamlineProcessor>> createProcessorProviders() {
        ImmutableMap.Builder<String, Provider<StreamlineProcessor>> builder = ImmutableMap.builder();
        builder.put(rulesProcessorProvider());
        builder.put(branchRulesProcessorProvider());
        builder.put(windowProcessorProvider());
        builder.put(normalizationProcessorProvider());
        builder.put(splitProcessorProvider());
        builder.put(joinProcessorProvider());
        builder.put(stageProcessorProvider());
        return builder.build();
    }

    /*
     * Its optional to register the specific child providers. Its needed only if
     * the specific child component exists and needs to be passed in the runtime.
     * In that case the specific child component would be added to the topology dag vertex.
     * Otherwise an instance of StreamlineSink is used.
     */
    private Map<String, Provider<StreamlineSink>> createSinkProviders() {
        ImmutableMap.Builder<String, Provider<StreamlineSink>> builder = ImmutableMap.builder();
        builder.put(notificationSinkProvider());
        return builder.build();
    }

    private Set<Stream> createOutputStreams(TopologyOutputComponent outputComponent) {
        Set<Stream> outputStreams = new HashSet<>();
        for (Long id : outputComponent.getOutputStreamIds()) {
            outputStreams.add(getStream(catalogService.getStreamInfo(outputComponent.getTopologyId(), id, outputComponent.getVersionId())));
        }
        return outputStreams;
    }

    private <T extends StreamlineComponent> Provider<T> getProvider(final Class<T> clazz, String type) {
        if (providerMap.get(clazz).containsKey(type)) {
            return (Provider<T>) providerMap.get(clazz).get(type);
        } else {
            LOG.warn("Type {} not found in provider map, returning an instance of {}", type, clazz.getName());
            return new Provider<T>() {
                @Override
                public T create(TopologyComponent component) {
                    try {
                        return clazz.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        LOG.error("Got exception ", e);
                        throw new RuntimeException(e);
                    }
                }
            };
        }
    }

    private interface Provider<T extends StreamlineComponent> {
        T create(TopologyComponent component);
    }

    private Map.Entry<String, Provider<StreamlineSource>> kafkaSourceProvider() {
        Provider<StreamlineSource> provider = new Provider<StreamlineSource>() {
            @Override
            public StreamlineSource create(TopologyComponent component) {
                return new KafkaSource();
            }
        };
        return new SimpleImmutableEntry<>(KAFKA, provider);
    }

    private Map.Entry<String, Provider<StreamlineProcessor>> normalizationProcessorProvider() {
        Provider<StreamlineProcessor> provider = new Provider<StreamlineProcessor>() {
            @Override
            public StreamlineProcessor create(TopologyComponent component) {
                Config config = component.getConfig();
                Object typeObj = config.getAny(NormalizationProcessor.CONFIG_KEY_TYPE);
                Object normConfObj = config.getAny(NormalizationProcessor.CONFIG_KEY_NORMALIZATION);
                ObjectMapper objectMapper = new ObjectMapper();
                NormalizationProcessor.Type type = objectMapper.convertValue(typeObj, NormalizationProcessor.Type.class);
                Map<String, NormalizationConfig> normConfig = objectMapper.convertValue(normConfObj, new TypeReference<Map<String, NormalizationConfig>>() {
                });
                updateWithSchemas(component.getTopologyId(), component.getVersionId(), normConfig);

                Set<Stream> outputStreams = createOutputStreams((TopologyOutputComponent) component);
                if (outputStreams.size() != 1) {
                    throw new IllegalArgumentException("Normalization component [" + component + "] must have only one output stream");
                }

                return new NormalizationProcessor(normConfig, outputStreams.iterator().next(), type);
            }
        };
        return new SimpleImmutableEntry<>(NORMALIZATION, provider);
    }

    private void updateWithSchemas(Long topologyId, Long versionId, Map<String, NormalizationConfig> normalizationConfigRead) {
        for (Map.Entry<String, NormalizationConfig> entry : normalizationConfigRead.entrySet()) {
            NormalizationConfig normalizationConfig = entry.getValue();
            StreamInfo streamInfo = catalogService.getStreamInfoByName(topologyId, entry.getKey(), versionId);
            if (streamInfo != null) {
                normalizationConfig.setInputSchema(streamInfo.getSchema());
            }
        }
    }

    private Map.Entry<String, Provider<StreamlineProcessor>> splitProcessorProvider() {
        Provider<StreamlineProcessor> provider = new Provider<StreamlineProcessor>() {
            @Override
            public StreamlineProcessor create(TopologyComponent component) {
                Object splitConfig = component.getConfig().getAny(SplitProcessor.CONFIG_KEY_SPLIT);
                ObjectMapper objectMapper = new ObjectMapper();
                SplitAction splitAction = objectMapper.convertValue(splitConfig, SplitAction.class);
                SplitProcessor splitProcessor = new SplitProcessor();
                splitProcessor.addOutputStreams(createOutputStreams((TopologyOutputComponent) component));
                splitProcessor.setSplitAction(splitAction);
                return splitProcessor;
            }
        };
        return new SimpleImmutableEntry<>(SPLIT, provider);
    }

    private Map.Entry<String, Provider<StreamlineProcessor>> joinProcessorProvider() {
        Provider<StreamlineProcessor> provider = new Provider<StreamlineProcessor>() {
            @Override
            public StreamlineProcessor create(TopologyComponent component) {
                Object joinConfig = component.getConfig().getAny(JoinProcessor.CONFIG_KEY_JOIN);
                ObjectMapper objectMapper = new ObjectMapper();
                JoinAction joinAction = objectMapper.convertValue(joinConfig, JoinAction.class);
                JoinProcessor joinProcessor = new JoinProcessor();
                joinProcessor.addOutputStreams(createOutputStreams((TopologyOutputComponent) component));
                joinProcessor.setJoinAction(joinAction);
                return joinProcessor;
            }
        };
        return new SimpleImmutableEntry<>(JOIN, provider);
    }

    private Map.Entry<String, Provider<StreamlineProcessor>> stageProcessorProvider() {
        Provider<StreamlineProcessor> provider = new Provider<StreamlineProcessor>() {
            @Override
            public StreamlineProcessor create(TopologyComponent component) {
                Object stageConfig = component.getConfig().getAny(StageProcessor.CONFIG_KEY_STAGE);
                ObjectMapper objectMapper = new ObjectMapper();
                StageAction stageAction = objectMapper.convertValue(stageConfig, StageAction.class);
                StageProcessor stageProcessor = new StageProcessor();
                stageProcessor.addOutputStreams(createOutputStreams((TopologyOutputComponent) component));
                stageProcessor.setStageAction(stageAction);
                return stageProcessor;
            }
        };
        return new SimpleImmutableEntry<>(STAGE, provider);
    }

    private interface RuleExtractor {
        Rule getRule(Long topologyId, Long ruleId, Long versionId) throws Exception;
    }

    private Map.Entry<String, Provider<StreamlineProcessor>> rulesProcessorProvider() {
        return new SimpleImmutableEntry<>(RULE, createRulesProcessorProvider(new RuleExtractor() {
            @Override
            public Rule getRule(Long topologyId, Long ruleId, Long versionId) throws Exception {
                RuleInfo ruleInfo = catalogService.getRule(topologyId, ruleId, versionId);
                if (ruleInfo == null) {
                    throw new IllegalArgumentException("Cannot find rule with id " + ruleId);
                }
                return ruleInfo.getRule();
            }
        }));
    }

    private Map.Entry<String, Provider<StreamlineProcessor>> branchRulesProcessorProvider() {
        return new SimpleImmutableEntry<>(BRANCH, createRulesProcessorProvider(new RuleExtractor() {
            @Override
            public Rule getRule(Long topologyId, Long ruleId, Long versionId) throws Exception {
                BranchRuleInfo brRuleInfo = catalogService.getBranchRule(topologyId, ruleId, versionId);
                if (brRuleInfo == null) {
                    throw new IllegalArgumentException("Cannot find branch rule with id " + ruleId);
                }
                return brRuleInfo.getRule();
            }
        }));
    }

    private Map.Entry<String, Provider<StreamlineProcessor>> windowProcessorProvider() {
        return new SimpleImmutableEntry<>(WINDOW, createRulesProcessorProvider(new RuleExtractor() {
            @Override
            public Rule getRule(Long topologyId, Long ruleId, Long versionId) throws Exception {
                WindowInfo windowInfo = catalogService.getWindow(topologyId, ruleId, versionId);
                if (windowInfo == null) {
                    throw new IllegalArgumentException("Cannot find window rule with id " + ruleId);
                }
                return windowInfo.getRule();
            }
        }));
    }

    private Provider<StreamlineProcessor> createRulesProcessorProvider(final RuleExtractor ruleExtractor) {
        return new Provider<StreamlineProcessor>() {
            @Override
            public StreamlineProcessor create(TopologyComponent component) {
                RulesProcessor processor = new RulesProcessor();
                ObjectMapper objectMapper = new ObjectMapper();

                Set<Stream> outputStreams = createOutputStreams((TopologyOutputComponent) component);
                processor.addOutputStreams(outputStreams);

                boolean processAll = component.getConfig().getBoolean(RulesProcessor.CONFIG_PROCESS_ALL, true);
                processor.setProcessAll(processAll);

                Object ruleList = component.getConfig().getAny(RulesProcessor.CONFIG_KEY_RULES);
                List<Long> ruleIds = objectMapper.convertValue(ruleList, new TypeReference<List<Long>>() {
                });
                try {
                    List<Rule> rules = new ArrayList<>();
                    for (Long ruleId : ruleIds) {
                        rules.add(ruleExtractor.getRule(component.getTopologyId(), ruleId, component.getVersionId()));
                    }
                    processor.setRules(rules);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                return processor;
            }
        };
    }


    private Map.Entry<String, Provider<StreamlineSink>> notificationSinkProvider() {
        Provider<StreamlineSink> provider = new Provider<StreamlineSink>() {
            @Override
            public StreamlineSink create(TopologyComponent component) {
                return new NotificationSink();
            }
        };
        return new SimpleImmutableEntry<>(NOTIFICATION, provider);
    }
}
