package org.apache.streamline.streams.catalog.service.metadata;


import org.apache.streamline.streams.catalog.Component;
import org.apache.streamline.streams.catalog.ServiceConfiguration;
import org.apache.streamline.streams.catalog.exception.ServiceComponentNotFoundException;
import org.apache.streamline.streams.catalog.exception.ServiceConfigurationNotFoundException;
import org.apache.streamline.streams.catalog.exception.ServiceNotFoundException;
import org.apache.streamline.streams.catalog.exception.ZookeeperClientException;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.catalog.service.metadata.common.HostPort;
import org.apache.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import org.apache.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private final StreamCatalogService catalogService;
    private final ZookeeperClient zkCli;
    private final KafkaZkConnection kafkaZkConnection;

    // package protected useful for unit tests
    KafkaMetadataService(StreamCatalogService catalogService, ZookeeperClient zkCli, KafkaZkConnection kafkaZkConnection) {
        this.catalogService = catalogService;
        this.zkCli = zkCli;
        this.kafkaZkConnection = kafkaZkConnection;
    }

    /**
     * Creates and starts a {@link ZookeeperClient} connection as part of the object construction process The connection must be
     * closed. See {@link KafkaMetadataService}
     */
    public static KafkaMetadataService newInstance(StreamCatalogService streamCatalogService, Long clusterId)
            throws ServiceConfigurationNotFoundException, IOException, ServiceNotFoundException {

        final KafkaZkConnection kafkaZkConnection = KafkaZkConnection.newInstance(getZkStringRaw(streamCatalogService, clusterId));
        final ZookeeperClient zkCli = ZookeeperClient.newInstance(kafkaZkConnection);
        zkCli.start();
        return new KafkaMetadataService(streamCatalogService, zkCli, kafkaZkConnection);
    }

    public BrokersInfo<HostPort> getBrokerHostPortFromStreamsJson(Long clusterId) throws ServiceNotFoundException, ServiceComponentNotFoundException {
        final Component kafkaBrokerComp = getKafkaBrokerComponent(clusterId);
        return BrokersInfo.hostPort(kafkaBrokerComp.getHosts(), kafkaBrokerComp.getPort());
    }

    public BrokersInfo<String> getBrokerInfoFromZk() throws ZookeeperClientException {
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
        return BrokersInfo.fromZk(brokerInfo);
    }

    public BrokersInfo<BrokersInfo.BrokerId> getBrokerIdsFromZk() throws ZookeeperClientException {
        final List<String> brokerIds = zkCli.getChildren(kafkaZkConnection.buildZkRootPath(KAFKA_BROKERS_IDS_ZK_RELATIVE_PATH));
        return BrokersInfo.brokerIds(brokerIds);
    }

    public Topics getTopicsFromZk() throws ZookeeperClientException {
        final List<String> topics = zkCli.getChildren(kafkaZkConnection.buildZkRootPath(KAFKA_TOPICS_ZK_RELATIVE_PATH));
        return topics == null ? new Topics(Collections.<String>emptyList()) : new Topics(topics);
    }

    @Override
    public void close() throws Exception {
        zkCli.close();
    }

    // ==== static methods used for object construction

    private static String getZkStringRaw(StreamCatalogService catalogService, Long clusterId)
            throws IOException, ServiceConfigurationNotFoundException, ServiceNotFoundException {

        final ServiceConfiguration kafkaBrokerConfig = catalogService.getServiceConfigurationByName(
                getKafkaServiceId(catalogService, clusterId), STREAMS_JSON_SCHEMA_CONFIG_KAFKA_BROKER);
        if (kafkaBrokerConfig == null || kafkaBrokerConfig.getConfigurationMap() == null) {
            throw new ServiceConfigurationNotFoundException(clusterId, ServiceConfigurations.KAFKA, STREAMS_JSON_SCHEMA_CONFIG_KAFKA_BROKER);
        }
        return kafkaBrokerConfig.getConfigurationMap().get(KAFKA_ZK_CONNECT_PROP);
    }

    private Component getKafkaBrokerComponent(Long clusterId) throws ServiceNotFoundException, ServiceComponentNotFoundException {
        Component component = catalogService.getComponentByName(getKafkaServiceId(catalogService, clusterId), STREAMS_JSON_SCHEMA_COMPONENT_KAFKA_BROKER);
        if (component == null) {
            throw new ServiceComponentNotFoundException(clusterId, ServiceConfigurations.KAFKA, ComponentPropertyPattern.KAFKA_BROKER);
        }
        return component;
    }

    private static Long getKafkaServiceId(StreamCatalogService catalogService, Long clusterId) throws ServiceNotFoundException {
        Long serviceId = catalogService.getServiceIdByName(clusterId, STREAMS_JSON_SCHEMA_SERVICE_KAFKA);
        if (serviceId == null) {
            throw new ServiceNotFoundException(clusterId, ServiceConfigurations.KAFKA);
        }
        return serviceId;
    }

    /**
     * Wrapper used to show proper JSON formatting {@code { "brokers" : [ { "host" : "H1", "port" : 23 }, { "host" : "H2", "port"
     * : 23 },{ "host" : "H3", "port" : 23 } ] }
     *
     * { "brokers" : [ { "id" : "1" }, { "id" : "2" }, { "id" : "3" } ] } }
     */

    public static class BrokersInfo<T> {
        private final List<T> brokers;

        public BrokersInfo(List<T> brokers) {
            this.brokers = brokers;
        }

        public static BrokersInfo<HostPort> hostPort(List<String> hosts, Integer port) {
            List<HostPort> hostsPorts = Collections.emptyList();
            if (hosts != null) {
                hostsPorts = new ArrayList<>(hosts.size());
                for (String host : hosts) {
                    hostsPorts.add(new HostPort(host, port));
                }
            }
            return new BrokersInfo<>(hostsPorts);
        }

        public static BrokersInfo<BrokerId> brokerIds(List<String> brokerIds) {
            List<BrokerId> brokerIdsType = Collections.emptyList();
            if (brokerIds != null) {
                brokerIdsType = new ArrayList<>(brokerIds.size());
                for (String brokerId : brokerIds) {
                    brokerIdsType.add(new BrokerId(brokerId));
                }
            }
            return new BrokersInfo<>(brokerIdsType);
        }

        public static BrokersInfo<String> fromZk(List<String> brokerInfo) {
            return brokerInfo == null
                    ? new BrokersInfo<>(Collections.<String>emptyList())
                    : new BrokersInfo<>(brokerInfo);
        }

        public List<T> getInfo() {
            return brokers;
        }

        public static class BrokerId {
            final String id;

            public BrokerId(String id) {
                this.id = id;
            }

            public String getId() {
                return id;
            }
        }
    }

    /**
     * Wrapper used to show proper JSON formatting
     */
    public static class Topics {
        final List<String> topics;

        public Topics(List<String> topics) {
            this.topics = topics;
        }

        public List<String> getTopics() {
            return topics;
        }
    }

    /**
     * Wrapper class used to represent zookeeper connection string (including chRoot) as defined in the kafka broker property
     * {@link KafkaMetadataService#KAFKA_ZK_CONNECT_PROP}
     */
    static class KafkaZkConnection implements ZookeeperClient.ZkConnectionStringFactory {
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
        static KafkaZkConnection newInstance(String zkStringRaw) {
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

        String getChRoot() {
            return chRoot;
        }
    }

}
