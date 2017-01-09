package com.hortonworks.streamline.streams.catalog.service.metadata;

import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.exception.ServiceComponentNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ServiceNotFoundException;
import com.hortonworks.streamline.streams.catalog.service.EnvironmentService;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.catalog.service.metadata.common.HostPort;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ZookeeperMetadataService {
    private static final String STREAMS_JSON_SCHEMA_SERVICE_ZOOKEEPER = ServiceConfigurations.ZOOKEEPER.name();
    private static final String STREAMS_JSON_SCHEMA_COMPONENT_ZOOKEEPER_SERVER = ComponentPropertyPattern.ZOOKEEPER_SERVER.name();

    private final EnvironmentService environmentService;
    private final Long clusterId;

    public ZookeeperMetadataService(EnvironmentService environmentService, Long clusterId) {
        this.environmentService = environmentService;
        this.clusterId = clusterId;
    }

    public List<HostPort> getZookeeperServers() throws ServiceNotFoundException, ServiceComponentNotFoundException {
        final Long serviceId = environmentService.getServiceIdByName(clusterId, STREAMS_JSON_SCHEMA_SERVICE_ZOOKEEPER);
        if (serviceId == null) {
            throw new ServiceNotFoundException(clusterId, ServiceConfigurations.ZOOKEEPER);
        }

        final Component zookeeperServer = environmentService.getComponentByName(serviceId, STREAMS_JSON_SCHEMA_COMPONENT_ZOOKEEPER_SERVER);

        if (zookeeperServer == null) {
            throw new ServiceComponentNotFoundException(clusterId, ServiceConfigurations.STORM, ComponentPropertyPattern.ZOOKEEPER_SERVER);
        }

        if (zookeeperServer.getHosts() != null) {
            return zookeeperServer.getHosts().stream()
                    .map(host -> new HostPort(host, zookeeperServer.getPort()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
