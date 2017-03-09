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

import com.google.common.collect.Lists;
import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.register.ManualServiceRegistrar;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.*;

public class KafkaServiceRegistrarTest extends AbstractServiceRegistrarTest<KafkaServiceRegistrar> {
    public static final String SERVER_PROPERTIES = "server.properties";
    public static final String SERVER_PROPERTIES_FILE_PATH = REGISTER_RESOURCE_DIRECTORY + SERVER_PROPERTIES;
    public static final String SERVER_PROPERTIES_BADCASE_FILE_PATH = REGISTER_BADCASE_RESOURCE_DIRECTORY + SERVER_PROPERTIES;
    public static final String COMPONENT_KAFKA_BROKER = "KAFKA_BROKER";
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

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(SERVER_PROPERTIES_FILE_PATH)) {
            Config config = new Config();
            config.put(KafkaServiceRegistrar.PARAM_KAFKA_BROKER_HOSTNAMES, "kafka-1,kafka-2");
            ManualServiceRegistrar.ConfigFileInfo serverProperties = new ManualServiceRegistrar.ConfigFileInfo(SERVER_PROPERTIES, is);
            registrar.register(cluster, config, Lists.newArrayList(serverProperties));
        }

        Service kafkaService = environmentService.getServiceByName(cluster.getId(), Constants.Kafka.SERVICE_NAME);
        assertNotNull(kafkaService);
        ServiceConfiguration serverPropertiesConf = environmentService.getServiceConfigurationByName(kafkaService.getId(), CONFIGURATION_NAME_SERVER_PROPERTIES);
        assertNotNull(serverPropertiesConf);
    }

    @Test
    public void testRegister_requiredPropertyNotPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        KafkaServiceRegistrar registrar = initializeServiceRegistrar();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(SERVER_PROPERTIES_BADCASE_FILE_PATH)) {
            Config config = new Config();
            config.put(KafkaServiceRegistrar.PARAM_KAFKA_BROKER_HOSTNAMES, "kafka-1,kafka-2");
            ManualServiceRegistrar.ConfigFileInfo serverProperties = new ManualServiceRegistrar.ConfigFileInfo(SERVER_PROPERTIES, is);
            registrar.register(cluster, config, Lists.newArrayList(serverProperties));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
            Service kafkaService = environmentService.getServiceByName(cluster.getId(), Constants.Kafka.SERVICE_NAME);
            assertNull(kafkaService);
        }
    }

    @Test
    public void testRegister_component_kafka_broker_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        KafkaServiceRegistrar registrar = initializeServiceRegistrar();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(SERVER_PROPERTIES_FILE_PATH)) {
            ManualServiceRegistrar.ConfigFileInfo serverProperties = new ManualServiceRegistrar.ConfigFileInfo(SERVER_PROPERTIES, is);
            registrar.register(cluster, new Config(), Lists.newArrayList(serverProperties));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
            Service kafkaService = environmentService.getServiceByName(cluster.getId(), Constants.Kafka.SERVICE_NAME);
            assertNull(kafkaService);
        }
    }

    @Test
    public void testRegister_server_properties_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        KafkaServiceRegistrar registrar = initializeServiceRegistrar();

        try {
            Config config = new Config();
            config.put(KafkaServiceRegistrar.PARAM_KAFKA_BROKER_HOSTNAMES, "kafka-1,kafka-2");
            registrar.register(cluster, config, Lists.newArrayList());
        } catch (IllegalArgumentException e) {
            // OK
            Service kafkaService = environmentService.getServiceByName(cluster.getId(), Constants.Kafka.SERVICE_NAME);
            assertNull(kafkaService);
        }
    }

}