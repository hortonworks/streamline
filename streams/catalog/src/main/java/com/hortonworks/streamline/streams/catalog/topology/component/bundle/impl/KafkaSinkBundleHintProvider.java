package org.apache.streamline.streams.catalog.topology.component.bundle.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.streamline.streams.catalog.Cluster;
import org.apache.streamline.streams.catalog.exception.ServiceConfigurationNotFoundException;
import org.apache.streamline.streams.catalog.exception.ServiceNotFoundException;
import org.apache.streamline.streams.catalog.service.metadata.KafkaMetadataService;
import org.apache.streamline.streams.catalog.service.metadata.common.HostPort;
import org.apache.streamline.streams.catalog.topology.component.bundle.AbstractBundleHintProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class KafkaSinkBundleHintProvider extends AbstractBundleHintProvider {
    public static final String SERVICE_NAME = "KAFKA";
    public static final String FIELD_NAME_TOPIC = "topic";
    public static final String FIELD_NAME_BOOTSTRAP_SERVERS = "bootstrapServers";
    public static final String FIELD_NAME_SECURITY_PROTOCOL = "securityProtocol";

    @Override
    public Map<String, Object> getHintsOnCluster(Cluster cluster) {
        Map<String, Object> hintClusterMap = new HashMap<>();
        try (KafkaMetadataService kafkaMetadataService = KafkaMetadataService.newInstance(environmentService, cluster.getId())) {
            KafkaMetadataService.Topics topics = kafkaMetadataService.getTopicsFromZk();
            hintClusterMap.put(FIELD_NAME_TOPIC, topics.getTopics());

            KafkaMetadataService.BrokersInfo<HostPort> brokerHosts = kafkaMetadataService.getBrokerHostPortFromStreamsJson(cluster.getId());
            List<HostPort> hosts = brokerHosts.getInfo();
            if (hosts != null && !hosts.isEmpty()) {
                List<String> bootstrapServerList = hosts.stream()
                        .map(hostPort -> String.format("%s:%d", hostPort.getHost(), hostPort.getPort()))
                        .collect(toList());
                hintClusterMap.put(FIELD_NAME_BOOTSTRAP_SERVERS, String.join(",", bootstrapServerList));
            }

            String protocol = kafkaMetadataService.getProtocolFromStreamsJson(cluster.getId());
            if (!StringUtils.isEmpty(protocol)) {
                hintClusterMap.put(FIELD_NAME_SECURITY_PROTOCOL, protocol);
            }
        } catch (ServiceNotFoundException e) {
            // we access it from mapping information so shouldn't be here
            throw new IllegalStateException("Service " + SERVICE_NAME + " in cluster " + cluster.getName() +
                    " not found but mapping information exists.");
        } catch (ServiceConfigurationNotFoundException e) {
            // there's KAFKA service but not enough configuration info.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return hintClusterMap;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }
}
