/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.streams.cluster.bundle.impl;

import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.exception.ServiceConfigurationNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ServiceNotFoundException;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.bundle.AbstractBundleHintProvider;
import com.hortonworks.streamline.streams.cluster.service.metadata.KafkaMetadataService;
import com.hortonworks.streamline.streams.cluster.service.metadata.ZookeeperMetadataService;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.HostPort;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaTopics;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class KafkaBundleHintProvider extends AbstractBundleHintProvider {
    public static final String DEFAULT_BROKER_ZK_PATH = "/brokers";

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
            KafkaTopics topics = kafkaMetadataService.getTopicsFromZk();

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
            hintClusterMap.put(FIELD_NAME_TOPIC, topics.list());
            hintClusterMap.put(FIELD_NAME_BROKER_ZK_PATH, brokerPath);

            fillZookeeperHints(cluster, hintClusterMap);
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
        return Constants.Kafka.SERVICE_NAME;
    }
}
