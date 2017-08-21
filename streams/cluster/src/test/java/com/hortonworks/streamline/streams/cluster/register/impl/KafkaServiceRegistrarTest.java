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
package com.hortonworks.streamline.streams.cluster.register.impl;

import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.cluster.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.catalog.Component;
import com.hortonworks.streamline.streams.cluster.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class KafkaServiceRegistrarTest extends AbstractServiceRegistrarTest<KafkaServiceRegistrar> {
    private static final String CONFIGURATION_NAME_SERVER_PROPERTIES = "server";

    public KafkaServiceRegistrarTest() {
        super(KafkaServiceRegistrar.class);
    }

    @Before
    public void setUp() {
        resetEnvironmentService();
    }

    @Test
    public void testRegister() throws Exception {
        Cluster cluster = getTestCluster(1L);

        KafkaServiceRegistrar registrar = initializeServiceRegistrar();

        Config config = new Config();
        config.put(KafkaServiceRegistrar.PARAM_ZOOKEEPER_CONNECT, "zookeeper-1:2181,zookeeper-2:2181,zookeeper-3:2181");
        config.put(KafkaServiceRegistrar.PARAM_LISTENERS, "SASL_PLAINTEXT://kafka-1:6668,PLAINTEXT://kafka-2:6669,SSL://kafka-3:6670,SASL_SSL://kafka-4:6671");
        config.put(KafkaServiceRegistrar.PARAM_SECURITY_INTER_BROKER_PROTOCOL, "SSL");
        registrar.register(cluster, config, Collections.emptyList());

        Service kafkaService = environmentService.getServiceByName(cluster.getId(), Constants.Kafka.SERVICE_NAME);
        assertNotNull(kafkaService);

        Component broker = environmentService.getComponentByName(kafkaService.getId(), ComponentPropertyPattern.KAFKA_BROKER.name());
        assertNotNull(broker);

        Collection<ComponentProcess> brokerProcesses = environmentService.listComponentProcesses(broker.getId());
        // we have 4 component processes according to listener
        assertEquals(4, brokerProcesses.size());
        assertTrue(brokerProcesses.stream().anyMatch(p -> p.getHost().equals("kafka-1") && p.getPort().equals(6668) && p.getProtocol().equals("SASL_PLAINTEXT")));
        assertTrue(brokerProcesses.stream().anyMatch(p -> p.getHost().equals("kafka-2") && p.getPort().equals(6669) && p.getProtocol().equals("PLAINTEXT")));
        assertTrue(brokerProcesses.stream().anyMatch(p -> p.getHost().equals("kafka-3") && p.getPort().equals(6670) && p.getProtocol().equals("SSL")));
        assertTrue(brokerProcesses.stream().anyMatch(p -> p.getHost().equals("kafka-4") && p.getPort().equals(6671) && p.getProtocol().equals("SASL_SSL")));

        ServiceConfiguration serverPropertiesConf = environmentService.getServiceConfigurationByName(kafkaService.getId(), CONFIGURATION_NAME_SERVER_PROPERTIES);
        assertNotNull(serverPropertiesConf);
        Map<String, String> serverPropertiesConfMap = serverPropertiesConf.getConfigurationMap();
        assertEquals(config.get(KafkaServiceRegistrar.PARAM_SECURITY_INTER_BROKER_PROTOCOL),
                serverPropertiesConfMap.get(KafkaServiceRegistrar.PARAM_SECURITY_INTER_BROKER_PROTOCOL));
    }

    @Test
    public void testRegisterWithoutOptionalParams() throws Exception {
        Cluster cluster = getTestCluster(1L);

        KafkaServiceRegistrar registrar = initializeServiceRegistrar();

        Config config = new Config();
        config.put(KafkaServiceRegistrar.PARAM_ZOOKEEPER_CONNECT, "zookeeper-1:2181,zookeeper-2:2181,zookeeper-3:2181");
        config.put(KafkaServiceRegistrar.PARAM_LISTENERS, "PLAINTEXT://kafka-1:9092");
        registrar.register(cluster, config, Collections.emptyList());

        Service kafkaService = environmentService.getServiceByName(cluster.getId(), Constants.Kafka.SERVICE_NAME);
        assertNotNull(kafkaService);

        Component broker = environmentService.getComponentByName(kafkaService.getId(), ComponentPropertyPattern.KAFKA_BROKER.name());
        assertNotNull(broker);

        Collection<ComponentProcess> brokerProcesses = environmentService.listComponentProcesses(broker.getId());
        assertEquals(1, brokerProcesses.size());
        assertTrue(brokerProcesses.stream().anyMatch(p -> p.getHost().equals("kafka-1") && p.getPort().equals(9092) && p.getProtocol().equals("PLAINTEXT")));

        ServiceConfiguration serverPropertiesConf = environmentService.getServiceConfigurationByName(kafkaService.getId(), CONFIGURATION_NAME_SERVER_PROPERTIES);
        assertNotNull(serverPropertiesConf);
        Map<String, String> serverPropertiesConfMap = serverPropertiesConf.getConfigurationMap();
        assertFalse(serverPropertiesConfMap.containsKey(KafkaServiceRegistrar.PARAM_SECURITY_INTER_BROKER_PROTOCOL));
    }

    @Test
    public void testRegister_component_zookeeper_connect_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        KafkaServiceRegistrar registrar = initializeServiceRegistrar();

        try {
            Config config = new Config();
            config.put(KafkaServiceRegistrar.PARAM_LISTENERS, "PLAINTEXT://kafka-1:9092");
            config.put(KafkaServiceRegistrar.PARAM_SECURITY_INTER_BROKER_PROTOCOL, "SSL");
            registrar.register(cluster, config, Collections.emptyList());
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
            Service kafkaService = environmentService.getServiceByName(cluster.getId(), Constants.Kafka.SERVICE_NAME);
            assertNull(kafkaService);
        }
    }

    @Test
    public void testRegister_component_listeners_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        KafkaServiceRegistrar registrar = initializeServiceRegistrar();

        try {
            Config config = new Config();
            config.put(KafkaServiceRegistrar.PARAM_ZOOKEEPER_CONNECT, "zookeeper-1:2181,zookeeper-2:2181,zookeeper-3:2181");
            config.put(KafkaServiceRegistrar.PARAM_SECURITY_INTER_BROKER_PROTOCOL, "SSL");
            registrar.register(cluster, config, Collections.emptyList());
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
            Service kafkaService = environmentService.getServiceByName(cluster.getId(), Constants.Kafka.SERVICE_NAME);
            assertNull(kafkaService);
        }
    }

}