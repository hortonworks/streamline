package org.apache.streamline.streams.layout.storm;

import com.google.common.collect.ImmutableMap;
import org.apache.streamline.streams.layout.component.StreamlineComponent;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static org.apache.streamline.common.ComponentTypes.CUSTOM;
import static org.apache.streamline.common.ComponentTypes.EVENTHUB;
import static org.apache.streamline.common.ComponentTypes.HBASE;
import static org.apache.streamline.common.ComponentTypes.HDFS;
import static org.apache.streamline.common.ComponentTypes.KAFKA;
import static org.apache.streamline.common.ComponentTypes.KINESIS;
import static org.apache.streamline.common.ComponentTypes.NORMALIZATION;
import static org.apache.streamline.common.ComponentTypes.NOTIFICATION;
import static org.apache.streamline.common.ComponentTypes.OPENTSDB;
import static org.apache.streamline.common.ComponentTypes.PARSER;

class FluxComponentFactory {
    private static final Logger LOG = LoggerFactory.getLogger(FluxComponentFactory.class);

    final Map<String, Provider<FluxComponent>> providerMap;

    FluxComponentFactory() {
        ImmutableMap.Builder<String, Provider<FluxComponent>> builder = ImmutableMap.builder();
        createProviders(builder);
        providerMap = builder.build();
    }

    FluxComponent getFluxComponent(StreamlineComponent component) {
        Provider<FluxComponent> provider = providerMap.get(component.getType());
        if (provider == null) {
            throw new IllegalArgumentException("Flux component provider is not registered for " + component.getType());
        }
        return provider.create(component);
    }

    private void createProviders(ImmutableMap.Builder<String, Provider<FluxComponent>> builder) {
        builder.put(KAFKA, provider(KafkaSpoutFluxComponent.class));
        builder.put(KINESIS, provider(KinesisSpoutFluxComponent.class));
        builder.put(EVENTHUB, provider(EventHubSpoutFluxComponent.class));
        builder.put(HBASE, provider(HbaseBoltFluxComponent.class));
        builder.put(HDFS, provider(HdfsBoltFluxComponent.class));
        builder.put(OPENTSDB, provider(OpenTsdbBoltFluxComponent.class));
        builder.put(NOTIFICATION, provider(NotificationBoltFluxComponent.class));
        builder.put(CUSTOM, provider(CustomProcessorBoltFluxComponent.class));
        builder.put(NORMALIZATION, provider(NormalizationBoltFluxComponent.class));
        builder.put(PARSER, provider(ParserBoltFluxComponent.class));
    }

    private Provider<FluxComponent> provider(final Class<? extends FluxComponent> clazz) {
        return new Provider<FluxComponent>() {
            @Override
            public FluxComponent create(StreamlineComponent component) {
                try {
                    Constructor<? extends FluxComponent> constructor =
                            ConstructorUtils.getAccessibleConstructor(clazz, component.getClass());
                    if (constructor != null) {
                        return constructor.newInstance(component);
                    }
                    return clazz.newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    LOG.error("Error while creating flux component", e);
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private interface Provider<T> {
        T create(StreamlineComponent component);
    }
}
