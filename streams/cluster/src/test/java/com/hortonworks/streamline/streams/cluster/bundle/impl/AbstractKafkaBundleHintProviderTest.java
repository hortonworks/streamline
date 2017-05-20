package com.hortonworks.streamline.streams.cluster.bundle.impl;

import com.google.common.collect.Lists;

import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.exception.ServiceNotFoundException;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import com.hortonworks.streamline.streams.cluster.service.metadata.KafkaMetadataService;
import com.hortonworks.streamline.streams.cluster.service.metadata.ZookeeperMetadataService;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.HostPort;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Authentication;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Authorizer;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaBrokerListeners;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaTopics;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Security;

import org.junit.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;

public class AbstractKafkaBundleHintProviderTest {
    protected AbstractKafkaBundleHintProvider provider;     // set in @Before setup method of subclass

    @Mocked
    protected StreamCatalogService catalogService;      //TODO

    @Mocked
    protected KafkaMetadataService kafkaMetadataService;

    @Mocked
    protected ZookeeperMetadataService zookeeperMetadataService;

    @Mocked
    protected Authentication authentication;

    @Mocked
    protected Authorizer authorizer;

    @Mocked
    protected Security security;

    protected void getHintsOnCluster() throws Exception {
        final List<String> topics = Lists.newArrayList("test1", "test2", "test3");

        final List<HostPort> zkHosts = Lists.newArrayList(new HostPort("svr1", 2181),
                new HostPort("svr2", 2181), new HostPort("svr3", 2181));

        final Map<KafkaBrokerListeners.Protocol, List<String>> protocolToHostsWithPort = getProtocolToHostsWithPort();

        new Expectations() {{
            kafkaMetadataService.getTopicsFromZk();
            result = new KafkaTopics(topics, security);

            zookeeperMetadataService.getZookeeperServers();
            result = zkHosts;

            kafkaMetadataService.getKafkaBrokerListeners().getProtocolToHostsWithPort();
            result = protocolToHostsWithPort;
        }};

        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cluster1");

        Map<String, Object> hints = provider.getHintsOnCluster(cluster, null, null);
        Assert.assertNotNull(hints);
        Assert.assertEquals(4, hints.size());
        Assert.assertEquals(topics, hints.get(KafkaBundleHintProvider.FIELD_NAME_TOPIC));
        Assert.assertEquals(AbstractKafkaBundleHintProvider.mapValuesJoiner(protocolToHostsWithPort, ","),
                hints.get(KafkaBundleHintProvider.FIELD_NAME_BOOTSTRAP_SERVERS));

        new Verifications() {{
            kafkaMetadataService.getTopicsFromZk();
            zookeeperMetadataService.getZookeeperServers();
        }};
    }

    private Map<KafkaBrokerListeners.Protocol, List<String>> getProtocolToHostsWithPort() {
        return new HashMap<KafkaBrokerListeners.Protocol, List<String>>() {{
            put(KafkaBrokerListeners.Protocol.PLAINTEXT, Lists.newArrayList("host1:6667", "host2:6667"));
            put(KafkaBrokerListeners.Protocol.SASL_PLAINTEXT, Lists.newArrayList("host1:6668", "host2:6668"));
        }};
    }

    protected void getHintsOnClusterWithZkServiceNotAvailable() throws Exception {
        final List<String> topics = Lists.newArrayList("test1", "test2", "test3");

        new Expectations() {{
            kafkaMetadataService.getTopicsFromZk();
            result = new KafkaTopics(topics, security);

            zookeeperMetadataService.getZookeeperServers();
            result = new ServiceNotFoundException(1L, ServiceConfigurations.ZOOKEEPER.name());
        }};

        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cluster1");

        Map<String, Object> hints = provider.getHintsOnCluster(cluster, null, null);
        Assert.assertNotNull(hints);
        Assert.assertEquals(4, hints.size());
        Assert.assertEquals(topics, hints.get(KafkaBundleHintProvider.FIELD_NAME_TOPIC));

        new Verifications() {{
            kafkaMetadataService.getTopicsFromZk();
            zookeeperMetadataService.getZookeeperServers();
        }};
    }

    protected void getServiceName() throws Exception {
        Assert.assertEquals(ServiceConfigurations.KAFKA.name(), provider.getServiceName());
    }
}
