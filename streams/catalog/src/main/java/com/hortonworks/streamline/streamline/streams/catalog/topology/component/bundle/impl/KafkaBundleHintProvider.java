package org.apache.streamline.streams.catalog.topology.component.bundle.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.streamline.streams.catalog.Cluster;
import org.apache.streamline.streams.catalog.exception.ServiceConfigurationNotFoundException;
import org.apache.streamline.streams.catalog.exception.ServiceNotFoundException;
import org.apache.streamline.streams.catalog.service.metadata.KafkaMetadataService;
import org.apache.streamline.streams.catalog.service.metadata.ZookeeperMetadataService;
import org.apache.streamline.streams.catalog.service.metadata.common.HostPort;
import org.apache.streamline.streams.catalog.topology.component.bundle.AbstractBundleHintProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class KafkaBundleHintProvider extends AbstractBundleHintProvider {
    public static final String DEFAULT_BROKER_ZK_PATH = "/brokers";

    public static final String SERVICE_NAME = "KAFKA";
    public static final String FIELD_NAME_ZK_URL = "zkUrl";
    public static final String FIELD_NAME_TOPIC = "topic";
    public static final String FIELD_NAME_BROKER_ZK_PATH = "zkPath";
    public static final String FIELD_NAME_ZK_SERVERS = "zkServers";
    public static final String FIELD_NAME_ZK_PORT = "zkPort";

    @Override
    public Map<String, Object> getHintsOnCluster(Cluster cluster) {
        Map<String, Object> hintClusterMap = new HashMap<>();
        try (KafkaMetadataService kafkaMetadataService = KafkaMetadataService.newInstance(environmentService, cluster.getId())) {
            KafkaMetadataService.KafkaZkConnection zkConnection = kafkaMetadataService.getKafkaZkConnection();
            KafkaMetadataService.Topics topics = kafkaMetadataService.getTopicsFromZk();

            String zkUrl = zkConnection.createZkConnString();
            String brokerPath = DEFAULT_BROKER_ZK_PATH;
            String zkRoot = zkConnection.getChRoot();
            if (StringUtils.isNotEmpty(zkRoot)) {
                if (zkRoot.endsWith("/")) {
                    brokerPath = zkRoot + brokerPath.substring(1);
                } else {
                    brokerPath = zkRoot + brokerPath;
                }
            }

            hintClusterMap.put(FIELD_NAME_ZK_URL, zkUrl);
            hintClusterMap.put(FIELD_NAME_TOPIC, topics.getTopics());
            hintClusterMap.put(FIELD_NAME_BROKER_ZK_PATH, brokerPath);

            fillZookeeperHints(cluster, hintClusterMap);
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

    private void fillZookeeperHints(Cluster cluster, Map<String, Object> hintClusterMap) {
        ZookeeperMetadataService zkMetadataService = new ZookeeperMetadataService(environmentService, cluster.getId());
        try {
            List<HostPort> zookeeperServers = zkMetadataService.getZookeeperServers();
            if (zookeeperServers != null && !zookeeperServers.isEmpty()) {
                List<String> hosts = zookeeperServers.stream().map(HostPort::getHost).collect(toList());
                hintClusterMap.put(FIELD_NAME_ZK_SERVERS, hosts);
                hintClusterMap.put(FIELD_NAME_ZK_PORT, zookeeperServers.get(0).getPort());
            }
        } catch (Exception e) {
            // not that important so just give up providing information
        }
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }
}
