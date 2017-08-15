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
import com.hortonworks.streamline.streams.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.catalog.exception.ServiceComponentNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ServiceConfigurationNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ServiceNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ZookeeperClientException;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.EnvironmentServiceUtil;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.HostPort;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Authorizer;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaBrokerListeners;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaBrokersInfo;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaTopics;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Keytabs;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Principals;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Security;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.ServicePrincipal;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.SecurityContext;

import static java.util.stream.Collectors.toList;

/**
 * This class opens zookeeper client connections which must be closed either by calling the {@link KafkaMetadataService#close()}'
 * method on a finally block, or instantiating this class in a try with resources statement.
 */
public class KafkaMetadataService implements AutoCloseable {
    private static final String AMBARI_JSON_SERVICE_KAFKA = ServiceConfigurations.KAFKA.name();
    private static final String AMBARI_JSON_COMPONENT_KAFKA_BROKER = ComponentPropertyPattern.KAFKA_BROKER.name();
    private static final String AMBARI_JSON_CONFIG_KAFKA_BROKER = ServiceConfigurations.KAFKA.getConfNames()[0];
    private static final String AMBARI_JSON_CONFIG_KAFKA_ENV = ServiceConfigurations.KAFKA.getConfNames()[1];

    static final String ZK_RELATIVE_PATH_KAFKA_TOPICS = "brokers/topics";
    static final String ZK_RELATIVE_PATH_KAFKA_BROKERS_IDS = "brokers/ids";
    static final String PROP_KAFKA_ZK_CONNECT = "zookeeper.connect";
    // Name of the service associated with the principal. {@see Principals}
    private static final String SERVICE_NAME_KAFKA = "kafka";

    private final ZookeeperClient zkCli;
    private final KafkaZkConnection kafkaZkConnection;
    private final SecurityContext securityContext;
    private final Component kafkaBroker;
    private final Collection<ComponentProcess> kafkaBrokerProcesses;
    private final ServiceConfiguration brokerConfig;
    private final ServiceConfiguration kafkaEnvConfig;

    // package protected useful for unit tests
    KafkaMetadataService(ZookeeperClient zkCli, KafkaZkConnection kafkaZkConnection,
                         SecurityContext securityContext, Component kafkaBroker, Collection<ComponentProcess> kafkaBrokerProcesses,
                         ServiceConfiguration brokerConfig, ServiceConfiguration kafkaEnvConfig) {
        this.zkCli = zkCli;
        this.kafkaZkConnection = kafkaZkConnection;
        this.securityContext = securityContext;
        this.kafkaBroker = kafkaBroker;
        this.kafkaBrokerProcesses = kafkaBrokerProcesses;
        this.brokerConfig = brokerConfig;
        this.kafkaEnvConfig = kafkaEnvConfig;
    }

    /**
     * Creates and starts a {@link ZookeeperClient} connection as part of the object construction process.
     * The connection must be closed. See {@link KafkaMetadataService}
     */
    public static KafkaMetadataService newInstance(
            EnvironmentService environmentService, Long clusterId, SecurityContext securityContext)
                throws ServiceConfigurationNotFoundException, IOException, ServiceNotFoundException, ServiceComponentNotFoundException {

        final KafkaZkConnection kafkaZkConnection = KafkaZkConnection.newInstance(
                getZkStringRaw(environmentService, clusterId, AMBARI_JSON_CONFIG_KAFKA_BROKER));
        final ZookeeperClient zkCli = ZookeeperClient.newInstance(kafkaZkConnection);
        zkCli.start();

        return new KafkaMetadataService(zkCli, kafkaZkConnection, securityContext,
                getKafkaBrokerComponent(environmentService, clusterId),
                getKafkaBrokers(environmentService, clusterId),
                getServiceConfig(environmentService, clusterId, AMBARI_JSON_CONFIG_KAFKA_BROKER),
                getServiceConfig(environmentService, clusterId, AMBARI_JSON_CONFIG_KAFKA_ENV));
    }

    public KafkaBrokersInfo<HostPort> getBrokerHostPortFromStreamsJson()
            throws ServiceNotFoundException, ServiceComponentNotFoundException, IOException {

        return KafkaBrokersInfo.hostPort(kafkaBrokerProcesses, getSecurity(), getKafkaBrokerListeners());
    }

    public KafkaBrokersInfo<String> getBrokerInfoFromZk() throws ZookeeperClientException, IOException {
        final String brokerIdsZkPath = kafkaZkConnection.buildZkRootPath(ZK_RELATIVE_PATH_KAFKA_BROKERS_IDS);
        final List<String> brokerIds = zkCli.getChildren(brokerIdsZkPath);
        List<String> brokerInfo = null;

        if (brokerIds != null) {
            brokerInfo = new ArrayList<>();
            for (String bkId : brokerIds) {
                final byte[] bytes = zkCli.getData(brokerIdsZkPath + "/" + bkId);
                brokerInfo.add(new String(bytes, "UTF-8"));
            }
        }
        return KafkaBrokersInfo.fromZk(brokerInfo, getSecurity(), getKafkaBrokerListeners());
    }

    public KafkaBrokersInfo<KafkaBrokersInfo.BrokerId> getBrokerIdsFromZk() throws ZookeeperClientException, IOException {
        final List<String> brokerIds = zkCli.getChildren(kafkaZkConnection.buildZkRootPath(ZK_RELATIVE_PATH_KAFKA_BROKERS_IDS));
        return KafkaBrokersInfo.brokerIds(brokerIds, getSecurity(), getKafkaBrokerListeners());
    }

    public KafkaTopics getTopicsFromZk() throws ZookeeperClientException, IOException {
        final Security security = getSecurity();
        final List<String> topics = zkCli.getChildren(kafkaZkConnection.buildZkRootPath(ZK_RELATIVE_PATH_KAFKA_TOPICS));
        return topics == null ? new KafkaTopics(Collections.emptyList(), security) : new KafkaTopics(topics, security);
    }

    public KafkaBrokerListeners getKafkaBrokerListeners() {
        return KafkaBrokerListeners.newInstance(brokerConfig, kafkaBroker, kafkaBrokerProcesses);
    }

    @Override
    public void close() throws Exception {
        zkCli.close();
    }

    public KafkaZkConnection getKafkaZkConnection() {
        return kafkaZkConnection;
    }

    public Keytabs getKeytabs() throws IOException {
        return Keytabs.fromAmbariConfig(kafkaEnvConfig);
    }

    public Principals getPrincipals() throws IOException {
        return Principals.fromAmbariConfig(kafkaEnvConfig, getServiceToComponent());
    }

    private Map<String, Pair<Component, Collection<ComponentProcess>>> getServiceToComponent() {
        return new HashMap<String, Pair<Component, Collection<ComponentProcess>>>(){{
            put("kafka", new Pair<>(kafkaBroker, kafkaBrokerProcesses));
        }};
    }

    public Security getSecurity() throws IOException {
        return new Security(securityContext, new Authorizer(false), getPrincipals(), getKeytabs());
    }

    /**
     * @return the name of the service associated with the principal. @See {@link Principals}
     */
    public String getKafkaServiceName() throws IOException {
        final List<Principal> kafkaPrincipals = getPrincipals().toMap().get(SERVICE_NAME_KAFKA);
        return kafkaPrincipals != null && !kafkaPrincipals.isEmpty()
                ? ((ServicePrincipal)kafkaPrincipals.get(0)).getService()
                : "";
    }

    // ==== static methods used for object construction

    private static String getZkStringRaw(EnvironmentService environmentService, Long clusterId, String configName)
            throws IOException, ServiceConfigurationNotFoundException, ServiceNotFoundException {

        return getServiceConfig(environmentService, clusterId, configName).getConfigurationMap().get(PROP_KAFKA_ZK_CONNECT);
    }

    private static ServiceConfiguration getServiceConfig(EnvironmentService environmentService, Long clusterId, String configName)
            throws ServiceNotFoundException, IOException, ServiceConfigurationNotFoundException {

        final ServiceConfiguration serviceConfig = environmentService.getServiceConfigurationByName(
                getKafkaServiceId(environmentService, clusterId), configName);

        if (serviceConfig == null || serviceConfig.getConfigurationMap() == null) {
            throw new ServiceConfigurationNotFoundException(clusterId, ServiceConfigurations.KAFKA.name(),
                    configName);
        }
        return serviceConfig;
    }

    private static Component getKafkaBrokerComponent(EnvironmentService environmentService, Long clusterId)
            throws ServiceNotFoundException, ServiceComponentNotFoundException {

        return EnvironmentServiceUtil.getComponent(
                environmentService, clusterId, AMBARI_JSON_SERVICE_KAFKA, AMBARI_JSON_COMPONENT_KAFKA_BROKER);
    }

    private static Collection<ComponentProcess> getKafkaBrokers(EnvironmentService environmentService, Long clusterId)
            throws ServiceNotFoundException, ServiceComponentNotFoundException {

        return EnvironmentServiceUtil.getComponentProcesses(
                environmentService, clusterId, AMBARI_JSON_SERVICE_KAFKA, AMBARI_JSON_COMPONENT_KAFKA_BROKER);
    }

    private static Long getKafkaServiceId(EnvironmentService environmentService, Long clusterId) throws ServiceNotFoundException {
        Long serviceId = environmentService.getServiceIdByName(clusterId, AMBARI_JSON_SERVICE_KAFKA);
        if (serviceId == null) {
            throw new ServiceNotFoundException(clusterId, AMBARI_JSON_SERVICE_KAFKA);
        }
        return serviceId;
    }

    /**
     * Wrapper class used to represent zookeeper connection string (including chRoot) as defined in the kafka broker property
     * {@link KafkaMetadataService#PROP_KAFKA_ZK_CONNECT}
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
