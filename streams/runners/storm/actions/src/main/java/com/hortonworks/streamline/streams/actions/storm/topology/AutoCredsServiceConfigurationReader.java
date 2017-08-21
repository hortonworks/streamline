package com.hortonworks.streamline.streams.actions.storm.topology;

import com.hortonworks.streamline.streams.cluster.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.NamespaceServiceClusterMapping;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.storm.common.ServiceConfigurationReadable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AutoCredsServiceConfigurationReader implements ServiceConfigurationReadable {

    private final EnvironmentService environmentService;
    private final long namespaceId;

    enum AutoCredsServiceConfigurations {
        // configurations will be added to specified order, so latter conf. might overwrite existing keys
        HDFS("core-site", "hadoop-env", "hadoop-policy", "hdfs-site"),
        HBASE("hbase-env", "hbase-policy", "hbase-site"),
        HIVE("hive-env", "hivemetastore-site", "hive-site");

        private final String[] confNames;

        AutoCredsServiceConfigurations(String... confNames) {
            this.confNames = confNames;
        }

        public String[] getConfNames() {
            return confNames;
        }
    }

    public AutoCredsServiceConfigurationReader(EnvironmentService environmentService, long namespaceId) {
        this.environmentService = environmentService;
        this.namespaceId = namespaceId;
    }

    @Override
    public Map<Long, Map<String, String>> readAllClusters(String serviceName) {
        Namespace namespace = environmentService.getNamespace(namespaceId);

        if (namespace == null) {
            throw new IllegalArgumentException("Namespace " + namespaceId + " doesn't exist.");
        }

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
        Cluster cluster = environmentService.getCluster(clusterId);
        if (cluster == null) {
            throw new IllegalArgumentException("Cluster with id " + clusterId + " doesn't exist.");
        }

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

        String[] confNames = AutoCredsServiceConfigurations.valueOf(serviceName).getConfNames();

        // let's forget about optimization here since two lists will be small enough
        Arrays.stream(confNames)
                .forEachOrdered(confName -> {
                    Optional<ServiceConfiguration> serviceConfigurationOptional = serviceConfigurations.stream()
                            .filter(sc -> sc.getName().equals(confName))
                            .findFirst();

                    if (serviceConfigurationOptional.isPresent()) {
                        ServiceConfiguration sc = serviceConfigurationOptional.get();
                        try {
                            Map<String, String> configurationMap = sc.getConfigurationMap();
                            flattenConfig.putAll(configurationMap);
                        } catch (IOException e) {
                            throw new RuntimeException("Can't read configuration from service configuration - ID: " + sc.getId());
                        }
                    }
                });

        return flattenConfig;
    }
}
