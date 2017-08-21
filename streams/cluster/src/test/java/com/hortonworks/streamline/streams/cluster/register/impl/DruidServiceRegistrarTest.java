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
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.Constants;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@RunWith(JMockit.class)
public class DruidServiceRegistrarTest extends AbstractServiceRegistrarTest<DruidServiceRegistrar> {
    private static final String CONFIGURATION_NAME_COMMON_RUNTIME_PROPERTIES = "common.runtime";

    public DruidServiceRegistrarTest() {
        super(DruidServiceRegistrar.class);
    }

    @Before
    public void setUp() {
        resetEnvironmentService();
    }

    @Test
    public void testRegister() throws Exception {
        Cluster cluster = getTestCluster(1L);

        DruidServiceRegistrar registerer = initializeServiceRegistrar();

        Config config = new Config();
        config.put(DruidServiceRegistrar.PARAM_ZOOKEEPER_CONNECTION_STRING, "zookeeper-1:2181,zookeeper-2:2181,zookeeper-3:2181");
        config.put(DruidServiceRegistrar.PARAM_INDEXING_SERVICE_NAME, "druid/overlord");
        config.put(DruidServiceRegistrar.PARAM_DISCOVERY_CURATOR_PATH, "/prod/discovery");

        registerer.register(cluster, config, Collections.emptyList());

        Service druidService = environmentService.getServiceByName(cluster.getId(), Constants.Druid.SERVICE_NAME);
        assertNotNull(druidService);

        ServiceConfiguration commonRuntimePropertiesConf = environmentService.getServiceConfigurationByName(druidService.getId(), CONFIGURATION_NAME_COMMON_RUNTIME_PROPERTIES);
        assertNotNull(commonRuntimePropertiesConf);

        Map<String, String> confMap = commonRuntimePropertiesConf.getConfigurationMap();
        assertEquals(config.get(DruidServiceRegistrar.PARAM_ZOOKEEPER_CONNECTION_STRING), confMap.get(DruidServiceRegistrar.PARAM_ZOOKEEPER_CONNECTION_STRING));
        assertEquals(config.get(DruidServiceRegistrar.PARAM_INDEXING_SERVICE_NAME), confMap.get(DruidServiceRegistrar.PARAM_INDEXING_SERVICE_NAME));
        assertEquals(config.get(DruidServiceRegistrar.PARAM_DISCOVERY_CURATOR_PATH), confMap.get(DruidServiceRegistrar.PARAM_DISCOVERY_CURATOR_PATH));
    }

    @Test
    public void testRegisterWithoutOptionalParams() throws Exception {
        Cluster cluster = getTestCluster(1L);

        DruidServiceRegistrar registerer = initializeServiceRegistrar();

        Config config = new Config();
        config.put(DruidServiceRegistrar.PARAM_ZOOKEEPER_CONNECTION_STRING, "zookeeper-1:2181,zookeeper-2:2181,zookeeper-3:2181");

        registerer.register(cluster, config, Collections.emptyList());

        Service druidService = environmentService.getServiceByName(cluster.getId(), Constants.Druid.SERVICE_NAME);
        assertNotNull(druidService);

        ServiceConfiguration commonRuntimePropertiesConf = environmentService.getServiceConfigurationByName(druidService.getId(), CONFIGURATION_NAME_COMMON_RUNTIME_PROPERTIES);
        assertNotNull(commonRuntimePropertiesConf);

        Map<String, String> confMap = commonRuntimePropertiesConf.getConfigurationMap();
        assertEquals(config.get(DruidServiceRegistrar.PARAM_ZOOKEEPER_CONNECTION_STRING), confMap.get(DruidServiceRegistrar.PARAM_ZOOKEEPER_CONNECTION_STRING));
    }

    @Test
    public void testRegister_zookeeper_connection_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        DruidServiceRegistrar registerer = initializeServiceRegistrar();

        try {
            Config config = new Config();
            config.put(DruidServiceRegistrar.PARAM_INDEXING_SERVICE_NAME, "druid/overlord");
            config.put(DruidServiceRegistrar.PARAM_DISCOVERY_CURATOR_PATH, "/prod/discovery");

            registerer.register(cluster, config, Collections.emptyList());
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
            Service druidService = environmentService.getServiceByName(cluster.getId(), Constants.Druid.SERVICE_NAME);
            assertNull(druidService);
        }
    }
}