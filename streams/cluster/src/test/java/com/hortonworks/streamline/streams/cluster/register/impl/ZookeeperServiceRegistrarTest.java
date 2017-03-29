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

public class ZookeeperServiceRegistrarTest extends AbstractServiceRegistrarTest<ZookeeperServiceRegistrar> {
    public static final String ZOO_CFG = "zoo.cfg";
    public static final String ZOO_CFG_FILE_PATH = REGISTER_RESOURCE_DIRECTORY + ZOO_CFG;
    public static final String ZOO_CFG_BADCASE_FILE_PATH = REGISTER_BADCASE_RESOURCE_DIRECTORY + ZOO_CFG;
    private static final String CONFIGURATION_NAME_ZOO_CFG = "zoo";

    public ZookeeperServiceRegistrarTest() {
        super(ZookeeperServiceRegistrar.class);
    }

    @Before
    public void setUp() {
        resetEnvironmentService();
    }

    @Test
    public void testRegister() throws Exception {
        Cluster cluster = getTestCluster(1L);

        ZookeeperServiceRegistrar registrar = initializeServiceRegistrar();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(ZOO_CFG_FILE_PATH)) {
            Config config = new Config();
            config.put(ZookeeperServiceRegistrar.PARAM_ZOOKEEPER_SERVER_HOSTNAMES, "zookeeper-1,zookeeper-2");
            ManualServiceRegistrar.ConfigFileInfo zooCfg = new ManualServiceRegistrar.ConfigFileInfo(ZOO_CFG, is);
            registrar.register(cluster, config, Lists.newArrayList(zooCfg));
        }

        Service zkService = environmentService.getServiceByName(cluster.getId(), Constants.Zookeeper.SERVICE_NAME);
        assertNotNull(zkService);
        ServiceConfiguration zooConf = environmentService.getServiceConfigurationByName(zkService.getId(), CONFIGURATION_NAME_ZOO_CFG);
        assertNotNull(zooConf);
    }

    @Test
    public void testRegister_requiredPropertyNotPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        ZookeeperServiceRegistrar registrar = initializeServiceRegistrar();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(ZOO_CFG_BADCASE_FILE_PATH)) {
            Config config = new Config();
            config.put(ZookeeperServiceRegistrar.PARAM_ZOOKEEPER_SERVER_HOSTNAMES, "zookeeper-1,zookeeper-2");
            ManualServiceRegistrar.ConfigFileInfo zooCfg = new ManualServiceRegistrar.ConfigFileInfo(ZOO_CFG, is);
            registrar.register(cluster, config, Lists.newArrayList(zooCfg));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
            Service zkService = environmentService.getServiceByName(cluster.getId(), Constants.Zookeeper.SERVICE_NAME);
            assertNull(zkService);
        }
    }

    @Test
    public void testRegister_component_zookeeper_server_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        ZookeeperServiceRegistrar registrar = initializeServiceRegistrar();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(ZOO_CFG_FILE_PATH)) {
            ManualServiceRegistrar.ConfigFileInfo zooCfg = new ManualServiceRegistrar.ConfigFileInfo(ZOO_CFG, is);
            registrar.register(cluster, new Config(), Lists.newArrayList(zooCfg));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
            Service zkService = environmentService.getServiceByName(cluster.getId(), Constants.Zookeeper.SERVICE_NAME);
            assertNull(zkService);
        }
    }

    @Test
    public void testRegister_zoo_cfg_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        ZookeeperServiceRegistrar registrar = initializeServiceRegistrar();

        try {
            Config config = new Config();
            config.put(ZookeeperServiceRegistrar.PARAM_ZOOKEEPER_SERVER_HOSTNAMES, "zookeeper-1,zookeeper-2");
            registrar.register(cluster, config, Lists.newArrayList());
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
            Service zkService = environmentService.getServiceByName(cluster.getId(), Constants.Zookeeper.SERVICE_NAME);
            assertNull(zkService);
        }
    }

}