package com.hortonworks.streamline.streams.cluster.bundle;

import com.google.common.base.Joiner;

import com.hortonworks.streamline.streams.cluster.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.exception.ServiceComponentNotFoundException;
import com.hortonworks.streamline.streams.cluster.exception.ServiceConfigurationNotFoundException;
import com.hortonworks.streamline.streams.cluster.exception.ServiceNotFoundException;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.service.metadata.KafkaMetadataService;
import com.hortonworks.streamline.streams.cluster.service.metadata.ZookeeperMetadataService;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.HostPort;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaBrokerListeners;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaTopics;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.security.auth.Subject;
import javax.ws.rs.core.SecurityContext;

import static java.util.stream.Collectors.toList;

public abstract class AbstractKafkaBundleHintProvider extends AbstractSecureBundleHintProvider {
    public static final String FIELD_NAME_TOPIC = "topic";
    public static final String FIELD_NAME_BOOTSTRAP_SERVERS = "bootstrapServers";
    public static final String FIELD_NAME_SECURITY_PROTOCOL = "securityProtocol";
    public static final String FIELD_NAME_KAFKA_SERVICE_NAME = "kafkaServiceName";

    @Override
    public Security getSecurity(Cluster cluster, SecurityContext securityContext, Subject subject) {
        try (KafkaMetadataService kms = KafkaMetadataService.newInstance(environmentService, cluster.getId(), securityContext)) {
            return kms.getSecurity();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> getHintsOnCluster(Cluster cluster, SecurityContext securityContext, Subject subject) {
        Map<String, Object> hintClusterMap = new HashMap<>();
        try (KafkaMetadataService kms = KafkaMetadataService.newInstance(environmentService, cluster.getId(), securityContext)) {
            KafkaTopics topics = kms.getTopicsFromZk();

            hintClusterMap.put(FIELD_NAME_TOPIC, topics.list());

            fillZookeeperHints(cluster, hintClusterMap);

            final Map<KafkaBrokerListeners.Protocol, List<String>> protocolToHostsWithPort =
                    kms.getKafkaBrokerListeners().getProtocolToHostsWithPort();

            hintClusterMap.put(FIELD_NAME_BOOTSTRAP_SERVERS, mapValuesJoiner(protocolToHostsWithPort, ","));
            hintClusterMap.put(FIELD_NAME_SECURITY_PROTOCOL, protocolToHostsWithPort.keySet());
            hintClusterMap.put(FIELD_NAME_KAFKA_SERVICE_NAME, kms.getKafkaServiceName());

        } catch (ServiceNotFoundException e) {
            // we access it from mapping information so shouldn't be here
            throw new IllegalStateException("Service " + Constants.Kafka.SERVICE_NAME + " in cluster " + cluster.getName() +
                    " not found but mapping information exists.");
        } catch (ServiceConfigurationNotFoundException e) {
            // there's KAFKA service but not enough configuration info.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return hintClusterMap;
    }

    /**
     * @return A map with the same keys, but with the map values list joined onto a String tokenized with the provided token.
     */
    public static Map<KafkaBrokerListeners.Protocol, String> mapValuesJoiner(
            Map<KafkaBrokerListeners.Protocol, List<String>> toJoin, String token) {

        return toJoin.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        (e) -> Joiner.on(token).join(e.getValue())));
    }

    private void fillZookeeperHints(Cluster cluster, Map<String, Object> hintClusterMap) {
        ZookeeperMetadataService zkMetadataService = new ZookeeperMetadataService(environmentService, cluster.getId());
        try {
            List<HostPort> zookeeperServers = zkMetadataService.getZookeeperServers();
            if (zookeeperServers != null && !zookeeperServers.isEmpty()) {
                List<String> hosts = zookeeperServers.stream().map(HostPort::getHost).collect(toList());
            }
        } catch (ServiceComponentNotFoundException | ServiceNotFoundException e) {
            // not that important so just give up providing information
        }
    }

    @Override
    public String getServiceName() {
        return Constants.Kafka.SERVICE_NAME;
    }
}
