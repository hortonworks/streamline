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
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaTopics;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Security;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class KafkaBundleHintProviderTest {

    private KafkaBundleHintProvider provider = new KafkaBundleHintProvider();

    @Mocked
    private StreamCatalogService catalogService;

    @Mocked
    private KafkaMetadataService kafkaMetadataService;

    @Mocked
    private ZookeeperMetadataService zookeeperMetadataService;

    @Mocked
    private Authentication authentication;

    @Mocked
    private Authorizer authorizer;

    @Mocked
    private Security security;

    @Test
    public void getHintsOnCluster() throws Exception {
        List<String> topics = Lists.newArrayList("test1", "test2", "test3");
        KafkaMetadataService.KafkaZkConnection zkConnection =
                KafkaMetadataService.KafkaZkConnection.newInstance("svr1:2181,svr2:2181,svr3:2181/root");

        List<HostPort> zkHosts = Lists.newArrayList(new HostPort("svr1", 2181),
                new HostPort("svr2", 2181), new HostPort("svr3", 2181));

        new Expectations() {{
            kafkaMetadataService.getTopicsFromZk();
            result = new KafkaTopics(topics, security);

            kafkaMetadataService.getKafkaZkConnection();
            result = zkConnection;

            zookeeperMetadataService.getZookeeperServers();
            result = zkHosts;
        }};

        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cluster1");

        Map<String, Object> hints = provider.getHintsOnCluster(cluster);
        Assert.assertNotNull(hints);
        Assert.assertEquals(5, hints.size());
        Assert.assertEquals(topics, hints.get(KafkaBundleHintProvider.FIELD_NAME_TOPIC));
        Assert.assertEquals("svr1:2181,svr2:2181,svr3:2181", hints.get(KafkaBundleHintProvider.FIELD_NAME_ZK_URL));
        Assert.assertEquals("/root/brokers", hints.get(KafkaBundleHintProvider.FIELD_NAME_BROKER_ZK_PATH));
        Assert.assertEquals(Lists.newArrayList("svr1", "svr2", "svr3"), hints.get(KafkaBundleHintProvider.FIELD_NAME_ZK_SERVERS));
        Assert.assertEquals(2181, hints.get(KafkaBundleHintProvider.FIELD_NAME_ZK_PORT));

        new Verifications() {{
            kafkaMetadataService.getTopicsFromZk();
            kafkaMetadataService.getKafkaZkConnection();
            zookeeperMetadataService.getZookeeperServers();
        }};
    }

    @Test
    public void getHintsOnClusterWithZkServiceNotAvailable() throws Exception {
        List<String> topics = Lists.newArrayList("test1", "test2", "test3");
        KafkaMetadataService.KafkaZkConnection zkConnection =
                KafkaMetadataService.KafkaZkConnection.newInstance("svr1:2181,svr2:2181,svr3:2181/root");

        new Expectations() {{
            kafkaMetadataService.getTopicsFromZk();
            result = new KafkaTopics(topics, security);

            kafkaMetadataService.getKafkaZkConnection();
            result = zkConnection;

            zookeeperMetadataService.getZookeeperServers();
            result = new ServiceNotFoundException(1L, ServiceConfigurations.ZOOKEEPER.name());
        }};

        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cluster1");

        Map<String, Object> hints = provider.getHintsOnCluster(cluster);
        Assert.assertNotNull(hints);
        Assert.assertEquals(3, hints.size());
        Assert.assertEquals(topics, hints.get(KafkaBundleHintProvider.FIELD_NAME_TOPIC));
        Assert.assertEquals("svr1:2181,svr2:2181,svr3:2181", hints.get(KafkaBundleHintProvider.FIELD_NAME_ZK_URL));
        Assert.assertEquals("/root/brokers", hints.get(KafkaBundleHintProvider.FIELD_NAME_BROKER_ZK_PATH));

        new Verifications() {{
            kafkaMetadataService.getTopicsFromZk();
            kafkaMetadataService.getKafkaZkConnection();
            zookeeperMetadataService.getZookeeperServers();
        }};
    }

    @Test
    public void getServiceName() throws Exception {
        Assert.assertEquals(ServiceConfigurations.KAFKA.name(), provider.getServiceName());
    }

}