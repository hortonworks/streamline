package com.hortonworks.streamline.streams.cluster.discovery.ambari;

import com.google.common.collect.Sets;
import com.hortonworks.streamline.streams.cluster.catalog.Cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ServiceConfigurationFilters {

    private static final String ATLAS_APPLICATION_PROPERTIES = "application-properties";
    private static final String ATLAS_CLUSTER_NAME_PATTERN = "{{cluster_name}}";
    private static final Set<String> ATLAS_REQUIRED_CONFIGS = Sets.newHashSet(
            "atlas.authentication.method.kerberos",
            "atlas.cluster.name",
            "atlas.hook.storm.numRetries",
            "atlas.jaas.KafkaClient.loginModuleControlFlag",
            "atlas.jaas.KafkaClient.loginModuleName",
            "atlas.jaas.KafkaClient.option.keyTab",
            "atlas.jaas.KafkaClient.option.principal",
            "atlas.jaas.KafkaClient.option.serviceName",
            "atlas.jaas.KafkaClient.option.storeKey",
            "atlas.jaas.KafkaClient.option.useKeyTab",
            "atlas.kafka.bootstrap.servers",
            "atlas.kafka.hook.group.id",
            "atlas.kafka.sasl.kerberos.service.name",
            "atlas.kafka.security.protocol",
            "atlas.kafka.zookeeper.connect",
            "atlas.kafka.zookeeper.connection.timeout.ms",
            "atlas.kafka.zookeeper.session.timeout.ms",
            "atlas.kafka.zookeeper.sync.time.ms",
            "atlas.notification.create.topics",
            "atlas.notification.replicas",
            "atlas.notification.topics",
            "atlas.rest.address"
    );

    public static SerivceConfigurationFilter get(Cluster cluster, String configType) {
        if (configType.equals(ATLAS_APPLICATION_PROPERTIES)) {
            return input -> {
                Map<String, String> result = new HashMap<>();
                input.forEach((k, v) -> {
                    if (ATLAS_REQUIRED_CONFIGS.contains(k)) {
                        if (v.equals(ATLAS_CLUSTER_NAME_PATTERN)) {
                            v = cluster.getName();
                        }
                        result.put(k, v);
                    }
                });
                return result;
            };
        }

        return input -> input;
    }
}
