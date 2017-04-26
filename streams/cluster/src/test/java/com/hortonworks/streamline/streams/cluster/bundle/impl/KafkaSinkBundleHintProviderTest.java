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
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import com.hortonworks.streamline.streams.cluster.service.metadata.KafkaMetadataService;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.HostPort;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Authentication;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Authorizer;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaBrokersInfo;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.KafkaTopics;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Security;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.SecurityContext;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class KafkaSinkBundleHintProviderTest {
    private static final String AUTHENTICATION_SCHEME_NOT_KERBEROS = "NOT_KERBEROS";
    private KafkaSinkBundleHintProvider provider = new KafkaSinkBundleHintProvider();

    @Mocked
    private StreamCatalogService catalogService;

    @Mocked
    private KafkaMetadataService kafkaMetadataService;

    @Injectable
    private SecurityContext securityContext;

    @Mocked
    private Authentication authentication;

    @Mocked
    private Authorizer authorizer;

    @Mocked
    private Security security;

    @Test
    public void getHintsOnCluster() throws Exception {
        new Expectations() {{
            securityContext.getAuthenticationScheme(); result = AUTHENTICATION_SCHEME_NOT_KERBEROS;
        }};

        List<String> topics = Lists.newArrayList("test1", "test2", "test3");

        List<String> hosts = Lists.newArrayList("svr1", "svr2");
        KafkaBrokersInfo<HostPort> brokersInfo = KafkaBrokersInfo.hostPort(hosts, 6667, securityContext);
        String protocol = "SASL_PLAINTEXT";

        new Expectations() {{
            kafkaMetadataService.getTopicsFromZk();
            result = new KafkaTopics(topics, security);

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