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
package com.hortonworks.streamline.streams.cluster.service.metadata;


import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.catalog.exception.ServiceComponentNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ServiceConfigurationNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ServiceNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ZookeeperClientException;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.HostPort;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Authorizer;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaBrokersInfo;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaTopics;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Security;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.SecurityContext;

import static java.util.stream.Collectors.toList;

/**
 * This class opens zookeeper client connections which must be closed either by calling the {@link KafkaMetadataService#close()}'
 * method on a finally block, or instantiating this class in a try with resources statement.
 */
public class KafkaMetadataService implements AutoCloseable {
    public static final String STREAMS_JSON_SCHEMA_SERVICE_KAFKA = ServiceConfigurations.KAFKA.name();
    public static final String STREAMS_JSON_SCHEMA_COMPONENT_KAFKA_BROKER = ComponentPropertyPattern.KAFKA_BROKER.name();
    public static final String STREAMS_JSON_SCHEMA_CONFIG_KAFKA_BROKER = ServiceConfigurations.KAFKA.getConfNames()[0];

    public static final String KAFKA_TOPICS_ZK_RELATIVE_PATH = "brokers/topics";
    public static final String KAFKA_BROKERS_IDS_ZK_RELATIVE_PATH = "brokers/ids";
    public static final String KAFKA_ZK_CONNECT_PROP = "zookeeper.connect";

    private final EnvironmentService environmentService;
    private final ZookeeperClient zkCli;
    private final KafkaZkConnection kafkaZkConnection;
    private SecurityContext securityContext;

    // package protected useful for unit tests
    KafkaMetadataService(EnvironmentService environmentService, ZookeeperClient zkCli,
                         KafkaZkConnection kafkaZkConnection, SecurityContext securityContext) {
        this.environmentService = environmentService;
        this.zkCli = zkCli;
        this.kafkaZkConnection = kafkaZkConnection;
        this.securityContext = securityContext;
    }

    public static KafkaMetadataService newInstance(EnvironmentService environmentService, Long clusterId)
            throws ServiceConfigurationNotFoundException, IOException, ServiceNotFoundException {
        return newInstance(environmentService, clusterId, null);
    }

    /**
     * Creates and starts a {@link ZookeeperClient} connection as part of the object construction process.
     * The connection must be closed. See {@link KafkaMetadataService}
     */
    public static KafkaMetadataService newInstance(EnvironmentService environmentService, Long clusterId,
            SecurityContext securityContext) throws ServiceConfigurationNotFoundException, IOException, ServiceNotFoundException {

        final KafkaZkConnection kafkaZkConnection = KafkaZkConnection.newInstance(getZkStringRaw(environmentService, clusterId));
        final ZookeeperClient zkCli = ZookeeperClient.newInstance(kafkaZkConnection);
        zkCli.start();
        return new KafkaMetadataService(environmentService, zkCli, kafkaZkConnection, securityContext);
    }

    public KafkaBrokersInfo<HostPort> getBrokerHostPortFromStreamsJson(Long clusterId) throws ServiceNotFoundException, ServiceComponentNotFoundException {
        final Component kafkaBrokerComp = getKafkaBrokerComponent(clusterId);
        return KafkaBrokersInfo.hostPort(kafkaBrokerComp.getHosts(), kafkaBrokerComp.getPort(), securityContext);
    }

    public String getProtocolFromStreamsJson(Long clusterId) throws ServiceNotFoundException, ServiceComponentNotFoundException {
        final Component kafkaBrokerComp = getKafkaBrokerComponent(clusterId);
        return kafkaBrokerComp.getProtocol();
    }

    public KafkaBrokersInfo<String> getBrokerInfoFromZk() throws ZookeeperClientException {
        final String brokerIdsZkPath = kafkaZkConnection.buildZkRootPath(KAFKA_BROKERS_IDS_ZK_RELATIVE_PATH);
        final List<String> brokerIds = zkCli.getChildren(brokerIdsZkPath);
        List<String> brokerInfo = null;

        if (brokerIds != null) {
            brokerInfo = new ArrayList<>();
            for (String bkId : brokerIds) {
                final byte[] bytes = zkCli.getData(brokerIdsZkPath + "/" + bkId);
                brokerInfo.add(new String(bytes));
            }
        }
        return KafkaBrokersInfo.fromZk(brokerInfo, securityContext);
    }

    public KafkaBrokersInfo<KafkaBrokersInfo.BrokerId> getBrokerIdsFromZk() throws ZookeeperClientException {
        final List<String> brokerIds = zkCli.getChildren(kafkaZkConnection.buildZkRootPath(KAFKA_BROKERS_IDS_ZK_RELATIVE_PATH));
        return KafkaBrokersInfo.brokerIds(brokerIds, securityContext);
    }

    public KafkaTopics getTopicsFromZk() throws ZookeeperClientException {
        final Security security = new Security(securityContext, new Authorizer(false));
        final List<String> topics = zkCli.getChildren(kafkaZkConnection.buildZkRootPath(KAFKA_TOPICS_ZK_RELATIVE_PATH));
        return topics == null ? new KafkaTopics(Collections.<String>emptyList(), security) : new KafkaTopics(topics, security);
    }

    @Override
    public void close() throws Exception {
        zkCli.close();
    }

    public KafkaZkConnection getKafkaZkConnection() {
        return kafkaZkConnection;
    }

    // ==== static methods used for object construction

    private static String getZkStringRaw(EnvironmentService environmentService, Long clusterId)
            throws IOException, ServiceConfigurationNotFoundException, ServiceNotFoundException {

        final ServiceConfiguration kafkaBrokerConfig = environmentService.getServiceConfigurationByName(
                getKafkaServiceId(environmentService, clusterId), STREAMS_JSON_SCHEMA_CONFIG_KAFKA_BROKER);
        if (kafkaBrokerConfig == null || kafkaBrokerConfig.getConfigurationMap() == null) {
            throw new ServiceConfigurationNotFoundException(clusterId, ServiceConfigurations.KAFKA.name(),
                    STREAMS_JSON_SCHEMA_CONFIG_KAFKA_BROKER);
        }
        return kafkaBrokerConfig.getConfigurationMap().get(KAFKA_ZK_CONNECT_PROP);
    }

    private Component getKafkaBrokerComponent(Long clusterId) throws ServiceNotFoundException, ServiceComponentNotFoundException {
        Component component = environmentService.getComponentByName(getKafkaServiceId(environmentService, clusterId), STREAMS_JSON_SCHEMA_COMPONENT_KAFKA_BROKER);
        if (component == null) {
            throw new ServiceComponentNotFoundException(clusterId, ServiceConfigurations.KAFKA.name(),
                    ComponentPropertyPattern.KAFKA_BROKER.name());
        }
        return component;
    }

    private static Long getKafkaServiceId(EnvironmentService environmentService, Long clusterId) throws ServiceNotFoundException {
        Long serviceId = environmentService.getServiceIdByName(clusterId, STREAMS_JSON_SCHEMA_SERVICE_KAFKA);
        if (serviceId == null) {
            throw new ServiceNotFoundException(clusterId, ServiceConfigurations.KAFKA.name());
        }
        return serviceId;
    }

    /**
     * Wrapper class used to represent zookeeper connection string (including chRoot) as defined in the kafka broker property
     * {@link KafkaMetadataService#KAFKA_ZK_CONNECT_PROP}
     */
    public static class KafkaZkConnection implements ZookeeperClient.ZkConnectionStringFactory {
        public static final int DEFAULT_ZOOKEEPER_PORT = 2181;
        final String zkString;
        final String chRoot;

        KafkaZkConnection(String zkString, String chRoot) {
            this.zkString = zkString;
            this.chRoot = chRoot;
        }

        /**
         * Factory method to create instance of {@link KafkaZkConnection} taking into consideration chRoot
         *
         * @param zkStringRaw zk connection string as defined in the broker zk property. It has the pattern
         *                    "hostname1:port1,hostname2:port2,hostname3:port3/chroot/path"
         */
        public static KafkaZkConnection newInstance(String zkStringRaw) {
            final String[] split = zkStringRaw.split("/", 2);
            String zkString;
            String chRoot;

            zkString = split[0];
            if (split.length > 1) {
                chRoot = "/" + split[1];
                if (!chRoot.endsWith("/")) {
                    chRoot = chRoot + "/";
                }
            } else {
                chRoot = "/";
            }
            return new KafkaZkConnection(zkString, chRoot);
        }

        @Override
        public String createZkConnString() {
            return zkString;
        }

        public List<HostPort> getZkHosts() {
            if (StringUtils.isEmpty(zkString)) {
                return Collections.emptyList();
            }

            return Arrays.stream(zkString.split(","))
                    .map(zkConn -> {
                        String[] splitted = zkConn.split(":");
                        if (splitted.length > 1) {
                            return new HostPort(splitted[0], Integer.valueOf(splitted[1]));
                        } else {
                            return new HostPort(splitted[0], DEFAULT_ZOOKEEPER_PORT);
                        }
                    })
                    .collect(toList());
        }

        String buildZkRootPath(String zkRelativePath) {
            if (zkRelativePath.startsWith("/")) {
                return chRoot + zkRelativePath.substring(1);
            } else {
                return chRoot + zkRelativePath;
            }
        }

        String getZkString() {
            return zkString;
        }

        public String getChRoot() {
            return chRoot;
        }
    }
}
