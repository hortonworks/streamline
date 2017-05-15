package com.hortonworks.streamline.streams.actions.storm.topology;

import com.google.common.collect.Lists;
import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Namespace;
import com.hortonworks.streamline.streams.catalog.NamespaceServiceClusterMapping;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.storm.common.ServiceConfigurationReadable;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ServiceConfigurationReader implements ServiceConfigurationReadable {

    private final EnvironmentService environmentService;
    private final long namespaceId;

    public ServiceConfigurationReader(EnvironmentService environmentService, long namespaceId) {
        this.environmentService = environmentService;
        this.namespaceId = namespaceId;
    }

    @Override
    public Map<Long, Map<String, String>> readAllClusters(String serviceName) {
        // FIXME: assertions or some validations

        Namespace namespace = environmentService.getNamespace(namespaceId);

        Long namespaceId = namespace.getId();

        Collection<NamespaceServiceClusterMapping> mappings = environmentService.listServiceClusterMapping(namespaceId, serviceName);

        List<Long> clusters = mappings.stream()
                .map(mapping -> mapping.getClusterId())
                .collect(Collectors.toList());

        Map<Long, Map<String, String>> retMap = new HashMap<>();
        clusters.forEach(c -> {
            Map<String, String> flattenConfig = read(c, serviceName);
            retMap.put(c, flattenConfig);
        });

        return retMap;
    }

    @Override
    public Map<String, String> read(Long clusterId, String serviceName) {
        Collection<NamespaceServiceClusterMapping> mappings = environmentService.listServiceClusterMapping(namespaceId, serviceName);
        boolean associated = mappings.stream().anyMatch(map -> map.getClusterId().equals(clusterId));
        if (!associated) {
            return Collections.emptyMap();
        }

        Long serviceId = environmentService.getServiceIdByName(clusterId, serviceName);
        if (serviceId == null) {
            throw new IllegalStateException("Cluster " + clusterId + " is associated to the service " + serviceName +
                " for namespace " + namespaceId + ", but actual service doesn't exist.");
        }

        Collection<ServiceConfiguration> serviceConfigurations = environmentService.listServiceConfigurations(serviceId);

        Map<String, String> flattenConfig = new HashMap<>();

        // FIXME: should resolve the priority between configurations if exists
        serviceConfigurations.forEach(sc -> {
            try {
                Map<String, String> configurationMap = sc.getConfigurationMap();
                flattenConfig.putAll(configurationMap);
            } catch (IOException e) {
                throw new RuntimeException("Can't read configuration from service configuration - ID: " + sc.getId());
            }
        });

        return flattenConfig;
    }

    @Override
    public Map<String, String> read(String clusterName, String serviceName) {
        Collection<Cluster> clusters = environmentService.listClusters(
                Lists.newArrayList(new QueryParam("namespaceId", String.valueOf(namespaceId)),
                        new QueryParam("name", clusterName)));

        // FIXME: assertions or some validations

        Cluster cluster = clusters.iterator().next();
        return read(cluster.getId(), serviceName);
    }
}
