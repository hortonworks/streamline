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
import com.hortonworks.streamline.streams.cluster.service.metadata.common.HostPort;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaBrokersInfo;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaTopics;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class KafkaSinkBundleHintProvider extends AbstractBundleHintProvider {
    public static final String FIELD_NAME_TOPIC = "topic";
    public static final String FIELD_NAME_BOOTSTRAP_SERVERS = "bootstrapServers";
    public static final String FIELD_NAME_SECURITY_PROTOCOL = "securityProtocol";

    @Override
    public Map<String, Object> getHintsOnCluster(Cluster cluster) {
        Map<String, Object> hintClusterMap = new HashMap<>();
        try (KafkaMetadataService kafkaMetadataService = KafkaMetadataService.newInstance(environmentService, cluster.getId())) {
            KafkaTopics topics = kafkaMetadataService.getTopicsFromZk();
            hintClusterMap.put(FIELD_NAME_TOPIC, topics.list());

            KafkaBrokersInfo<HostPort> brokerHosts = kafkaMetadataService.getBrokerHostPortFromStreamsJson(cluster.getId());
            List<HostPort> hosts = brokerHosts.getBrokers();
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
            throw new IllegalStateException("Service " + Constants.Kafka.SERVICE_NAME + " in cluster " + cluster.getName() +
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
        return Constants.Kafka.SERVICE_NAME;
    }
}
