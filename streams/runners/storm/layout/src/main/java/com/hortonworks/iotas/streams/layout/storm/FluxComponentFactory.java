package com.hortonworks.iotas.streams.layout.storm;

import com.google.common.collect.ImmutableMap;
import com.hortonworks.iotas.streams.layout.component.IotasComponent;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static com.hortonworks.iotas.common.ComponentTypes.*;

class FluxComponentFactory {
    private static final Logger LOG = LoggerFactory.getLogger(FluxComponentFactory.class);

    Map<String, Provider<FluxComponent>> providerMap;

    FluxComponentFactory() {
        ImmutableMap.Builder<String, Provider<FluxComponent>> builder = ImmutableMap.builder();
        createProviders(builder);
        providerMap = builder.build();
    }

    FluxComponent getFluxComponent(IotasComponent iotasComponent) {
        Provider<FluxComponent> provider = providerMap.get(iotasComponent.getType());
        if (provider == null) {
            throw new IllegalArgumentException("Flux component provider is not registered for " + iotasComponent.getType());
        }
        return provider.create(iotasComponent);
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
            public FluxComponent create(IotasComponent component) {
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
        T create(IotasComponent component);
    }
}
