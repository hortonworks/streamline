package com.hortonworks.streamline.streams.layout.storm;

import com.hortonworks.streamline.common.util.ProxyUtil;
import com.hortonworks.streamline.streams.layout.component.StreamlineComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.MalformedURLException;
import java.nio.file.Path;

class FluxComponentFactory {
    private static final Logger LOG = LoggerFactory.getLogger(FluxComponentFactory.class);

    private final Path extraJarsLocation;
    FluxComponentFactory(Path extraJarsLocation) {
        this.extraJarsLocation = extraJarsLocation;
    }

    FluxComponent getFluxComponent(StreamlineComponent streamlineComponent) {
        ProxyUtil<FluxComponent> proxyUtil = new ProxyUtil<>(FluxComponent.class);
        try {
            FluxComponent fluxComponent = proxyUtil.loadClassFromJar(extraJarsLocation.toAbsolutePath().toString(), streamlineComponent.getTransformationClass());
            return fluxComponent;
        } catch (ClassNotFoundException | MalformedURLException | InstantiationException | IllegalAccessException e) {
            LOG.error("Error while creating flux component", e);
            throw new RuntimeException(e);
        }
    }
}
