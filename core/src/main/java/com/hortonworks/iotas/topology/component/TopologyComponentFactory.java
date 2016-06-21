package com.hortonworks.iotas.topology.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.hortonworks.iotas.catalog.StreamInfo;
import com.hortonworks.iotas.catalog.TopologyComponent;
import com.hortonworks.iotas.catalog.TopologyEdge;
import com.hortonworks.iotas.catalog.TopologyOutputComponent;
import com.hortonworks.iotas.catalog.TopologyProcessor;
import com.hortonworks.iotas.catalog.TopologySink;
import com.hortonworks.iotas.catalog.TopologySource;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.topology.component.impl.CustomProcessor;
import com.hortonworks.iotas.topology.component.impl.HbaseSink;
import com.hortonworks.iotas.topology.component.impl.HdfsSink;
import com.hortonworks.iotas.topology.component.impl.KafkaSource;
import com.hortonworks.iotas.topology.component.impl.NotificationSink;
import com.hortonworks.iotas.topology.component.impl.ParserProcessor;
import com.hortonworks.iotas.topology.component.impl.RulesProcessor;
import com.hortonworks.iotas.topology.component.rule.Rule;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Constructs various topology components based on the
 * TopologyComponent catalog entities
 */
public class TopologyComponentFactory {
    private final Map<Class<?>, Map<String, ?>> providerMap;
    private final CatalogService catalogService;

    public TopologyComponentFactory(CatalogService catalogService) {
        this.catalogService = catalogService;
        ImmutableMap.Builder<Class<?>, Map<String, ?>> builder = ImmutableMap.builder();
        builder.put(IotasSource.class, createSourceProviders());
        builder.put(IotasProcessor.class, createProcessorProviders());
        builder.put(IotasSink.class, createSinkProviders());
        providerMap = builder.build();
    }

    public IotasSource getIotasSource(TopologySource topologySource) {
        IotasSource source = getProvider(IotasSource.class, topologySource.getType()).create(topologySource);
        source.setId(topologySource.getId().toString());
        source.setName(topologySource.getName());
        source.setConfig(topologySource.getConfig());
        source.addOutputStreams(createOutputStreams(topologySource));
        return source;
    }

    public IotasProcessor getIotasProcessor(TopologyProcessor topologyProcessor) {
        IotasProcessor processor = getProvider(IotasProcessor.class, topologyProcessor.getType()).create(topologyProcessor);
        processor.setId(topologyProcessor.getId().toString());
        processor.setName(topologyProcessor.getName());
        processor.setConfig(topologyProcessor.getConfig());
        processor.addOutputStreams(createOutputStreams(topologyProcessor));
        return processor;
    }

    public IotasSink getIotasSink(TopologySink topologySink) {
        IotasSink sink = getProvider(IotasSink.class, topologySink.getType()).create(topologySink);
        sink.setId(topologySink.getId().toString());
        sink.setName(topologySink.getName());
        sink.setConfig(topologySink.getConfig());
        return sink;
    }

    public Edge getIotasEdge(TopologyEdge topologyEdge) {
        Edge edge = new Edge();
        edge.setFrom(getOutputComponent(topologyEdge));
        edge.setTo(getInputComponent(topologyEdge));
        Set<StreamGrouping> streamGroupings = new HashSet<>();
        for (TopologyEdge.StreamGrouping streamGrouping: topologyEdge.getStreamGroupings()) {
            Stream stream = getStream(catalogService.getStreamInfo(streamGrouping.getStreamId()));
            Stream.Grouping grouping = Stream.Grouping.valueOf(streamGrouping.getGrouping().name());
            streamGroupings.add(new StreamGrouping(stream, grouping, streamGrouping.getFields()));
        }
        edge.addStreamGroupings(streamGroupings);
        return edge;
    }

    private OutputComponent getOutputComponent(TopologyEdge topologyEdge) {
        TopologySource topologySource;
        TopologyProcessor topologyProcessor;
        if ((topologySource = catalogService.getTopologySource(topologyEdge.getFromId())) != null) {
            return getIotasSource(topologySource);
        } else if ((topologyProcessor = catalogService.getTopologyProcessor(topologyEdge.getFromId())) != null) {
            return getIotasProcessor(topologyProcessor);
        } else {
            throw new IllegalArgumentException("Invalid from id for edge " + topologyEdge);
        }
    }

    private InputComponent getInputComponent(TopologyEdge topologyEdge) {
        TopologySink topologySink;
        TopologyProcessor topologyProcessor;
        if ((topologySink = catalogService.getTopologySink(topologyEdge.getToId())) != null) {
            return getIotasSink(topologySink);
        } else if ((topologyProcessor = catalogService.getTopologyProcessor(topologyEdge.getToId())) != null) {
            return getIotasProcessor(topologyProcessor);
        } else {
            throw new IllegalArgumentException("Invalid to id for edge " + topologyEdge);
        }
    }

    public Stream getStream(StreamInfo streamInfo) {
        return new Stream(streamInfo.getStreamId(), streamInfo.getFields());
    }

    private Map<String, Provider<IotasSource>> createSourceProviders() {
        ImmutableMap.Builder<String, Provider<IotasSource>> builder = ImmutableMap.builder();
        builder.put(kafkaSourceProvider());
        return builder.build();
    }

    private Map<String, Provider<IotasProcessor>> createProcessorProviders() {
        ImmutableMap.Builder<String, Provider<IotasProcessor>> builder = ImmutableMap.builder();
        builder.put(rulesProcessorProvider());
        builder.put(parserProcessorProvider());
        builder.put(customProcessorProvider());
        return builder.build();
    }

    private Map<String, Provider<IotasSink>> createSinkProviders() {
        ImmutableMap.Builder<String, Provider<IotasSink>> builder = ImmutableMap.builder();
        builder.put(hbaseSinkProvider());
        builder.put(hdfsSinkProvider());
        builder.put(notificationSinkProvider());
        return builder.build();
    }

    private Set<Stream> createOutputStreams(TopologyOutputComponent outputComponent) {
        Set<Stream> outputStreams = new HashSet<>();
        for (Long id : outputComponent.getOutputStreamIds()) {
            outputStreams.add(getStream(catalogService.getStreamInfo(id)));
        }
        return outputStreams;
    }

    private <T extends IotasComponent> Provider<T> getProvider(Class<T> clazz, String type) {
        if (providerMap.get(clazz).containsKey(type)) {
            return (Provider<T>) providerMap.get(clazz).get(type);
        }
        throw new UnsupportedOperationException("Unknown type " + type);
    }

    private interface Provider<T extends IotasComponent> {
        T create(TopologyComponent component);
    }

    private Map.Entry<String, Provider<IotasSource>> kafkaSourceProvider() {
        Provider<IotasSource> provider = new Provider<IotasSource>() {
            @Override
            public IotasSource create(TopologyComponent component) {
                return new KafkaSource();
            }
        };
        return new AbstractMap.SimpleImmutableEntry<>("KAFKA", provider);
    }

    private Map.Entry<String, Provider<IotasProcessor>> rulesProcessorProvider() {
        Provider<IotasProcessor> provider = new Provider<IotasProcessor>() {
            @Override
            public IotasProcessor create(TopologyComponent component) {
                RulesProcessor processor = new RulesProcessor();
                ObjectMapper objectMapper = new ObjectMapper();
                Object ruleList = component.getConfig().getAny(RulesProcessor.CONFIG_KEY_RULES);
                List<Rule> rules = objectMapper.convertValue(ruleList, new TypeReference<List<Rule>>() {});
                processor.setRules(rules);
                return processor;
            }
        };
        return new AbstractMap.SimpleImmutableEntry<>("RULE", provider);
    }

    private Map.Entry<String, Provider<IotasProcessor>> parserProcessorProvider() {
        Provider<IotasProcessor> provider = new Provider<IotasProcessor>() {
            @Override
            public IotasProcessor create(TopologyComponent component) {
                return new ParserProcessor();
            }
        };
        return new AbstractMap.SimpleImmutableEntry<>("PARSER", provider);
    }

    private Map.Entry<String, Provider<IotasProcessor>> customProcessorProvider() {
        Provider<IotasProcessor> provider = new Provider<IotasProcessor>() {
            @Override
            public IotasProcessor create(TopologyComponent component) {
                return new CustomProcessor();
            }
        };
        return new AbstractMap.SimpleImmutableEntry<>("CUSTOM", provider);
    }


    private Map.Entry<String, Provider<IotasSink>> hbaseSinkProvider() {
        Provider<IotasSink> provider = new Provider<IotasSink>() {
            @Override
            public IotasSink create(TopologyComponent component) {
                return new HbaseSink();
            }
        };
        return new AbstractMap.SimpleImmutableEntry<>("HBASE", provider);
    }

    private Map.Entry<String, Provider<IotasSink>> hdfsSinkProvider() {
        Provider<IotasSink> provider = new Provider<IotasSink>() {
            @Override
            public IotasSink create(TopologyComponent component) {
                return new HdfsSink();
            }
        };
        return new AbstractMap.SimpleImmutableEntry<>("HDFS", provider);
    }

    private Map.Entry<String, Provider<IotasSink>> notificationSinkProvider() {
        Provider<IotasSink> provider = new Provider<IotasSink>() {
            @Override
            public IotasSink create(TopologyComponent component) {
                return new NotificationSink();
            }
        };
        return new AbstractMap.SimpleImmutableEntry<>("NOTIFICATION", provider);
    }
}
