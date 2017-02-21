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
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.register.ManualServiceRegisterer;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

public class KafkaServiceRegistererTest extends AbstractServiceRegistererTest<KafkaServiceRegisterer> {
    public static final String SERVER_PROPERTIES = "server.properties";
    public static final String SERVER_PROPERTIES_FILE_PATH = REGISTER_RESOURCE_DIRECTORY + SERVER_PROPERTIES;
    public static final String SERVER_PROPERTIES_BADCASE_FILE_PATH = REGISTER_BADCASE_RESOURCE_DIRECTORY + SERVER_PROPERTIES;
    public static final String COMPONENT_KAFKA_BROKER = "KAFKA_BROKER";

    public KafkaServiceRegistererTest() {
        super(KafkaServiceRegisterer.class);
    }

    @Before
    public void setUp() {
        resetEnvironmentService();
    }

    @Test
    public void testRegister_happyCase() throws Exception {
        Cluster cluster = getTestCluster(1L);

        KafkaServiceRegisterer registerer = initializeServiceRegisterer();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(SERVER_PROPERTIES_FILE_PATH)) {
            List<ManualServiceRegisterer.ComponentInfo> components = Lists.newArrayList(
                    new ManualServiceRegisterer.ComponentInfo(COMPONENT_KAFKA_BROKER, Lists.newArrayList("kafka-1", "kafka-2"))
            );
            ManualServiceRegisterer.ConfigFileInfo serverProperties = new ManualServiceRegisterer.ConfigFileInfo(SERVER_PROPERTIES, is);
            registerer.register(cluster, components, Lists.newArrayList(serverProperties));
        }
    }

    @Test
    public void testRegister_requiredPropertyNotPresented() throws Exception {
        Cluster cluster = getTestCluster(1L);

        KafkaServiceRegisterer registerer = initializeServiceRegisterer();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(SERVER_PROPERTIES_BADCASE_FILE_PATH)) {
            List<ManualServiceRegisterer.ComponentInfo> components = Lists.newArrayList(
                    new ManualServiceRegisterer.ComponentInfo(COMPONENT_KAFKA_BROKER, Lists.newArrayList("kafka-1", "kafka-2"))
            );
            ManualServiceRegisterer.ConfigFileInfo serverProperties = new ManualServiceRegisterer.ConfigFileInfo(SERVER_PROPERTIES, is);
            registerer.register(cluster, components, Lists.newArrayList(serverProperties));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testRegister_component_kafka_broker_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        KafkaServiceRegisterer registerer = initializeServiceRegisterer();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(SERVER_PROPERTIES_FILE_PATH)) {
            ManualServiceRegisterer.ConfigFileInfo serverProperties = new ManualServiceRegisterer.ConfigFileInfo(SERVER_PROPERTIES, is);
            registerer.register(cluster, Lists.newArrayList(), Lists.newArrayList(serverProperties));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(COMPONENT_KAFKA_BROKER));
        }
    }

    @Test
    public void testRegister_server_properties_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        KafkaServiceRegisterer registerer = initializeServiceRegisterer();

        try {
            List<ManualServiceRegisterer.ComponentInfo> components = Lists.newArrayList(
                    new ManualServiceRegisterer.ComponentInfo(COMPONENT_KAFKA_BROKER, Lists.newArrayList("kafka-1", "kafka-2"))
            );
            registerer.register(cluster, components, Lists.newArrayList());
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(SERVER_PROPERTIES));
        }
    }

}