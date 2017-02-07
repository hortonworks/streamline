package com.hortonworks.streamline.streams.cluster.bundle.impl;

import com.google.common.collect.Lists;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.cluster.service.metadata.KafkaMetadataService;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.HostPort;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;

@RunWith(JMockit.class)
public class KafkaSinkBundleHintProviderTest {
    private KafkaSinkBundleHintProvider provider = new KafkaSinkBundleHintProvider();

    @Mocked
    private StreamCatalogService catalogService;

    @Mocked
    private KafkaMetadataService kafkaMetadataService;

    @Test
    public void getHintsOnCluster() throws Exception {
        List<String> topics = Lists.newArrayList("test1", "test2", "test3");

        List<String> hosts = Lists.newArrayList("svr1", "svr2");
        KafkaMetadataService.BrokersInfo<HostPort> brokersInfo = KafkaMetadataService.BrokersInfo.hostPort(hosts, 6667);
        String protocol = "SASL_PLAINTEXT";

        new Expectations() {{
            kafkaMetadataService.getTopicsFromZk();
            result = new KafkaMetadataService.Topics(topics);

            kafkaMetadataService.getBrokerHostPortFromStreamsJson(1L);
            result = brokersInfo;

            kafkaMetadataService.getProtocolFromStreamsJson(1L);
            result = protocol;
        }};

        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cluster1");

        Map<String, Object> hints = provider.getHintsOnCluster(cluster);
        Assert.assertNotNull(hints);
        Assert.assertEquals(3, hints.size());
        Assert.assertEquals(topics, hints.get(KafkaSinkBundleHintProvider.FIELD_NAME_TOPIC));
        Assert.assertEquals("svr1:6667,svr2:6667", hints.get(KafkaSinkBundleHintProvider.FIELD_NAME_BOOTSTRAP_SERVERS));
        Assert.assertEquals(protocol, hints.get(KafkaSinkBundleHintProvider.FIELD_NAME_SECURITY_PROTOCOL));

        new Verifications() {{
            kafkaMetadataService.getTopicsFromZk();
            kafkaMetadataService.getBrokerHostPortFromStreamsJson(1L);
            kafkaMetadataService.getProtocolFromStreamsJson(1L);
        }};
    }

    @Test
    public void getServiceName() throws Exception {
        Assert.assertEquals(ServiceConfigurations.KAFKA.name(), provider.getServiceName());
    }

}