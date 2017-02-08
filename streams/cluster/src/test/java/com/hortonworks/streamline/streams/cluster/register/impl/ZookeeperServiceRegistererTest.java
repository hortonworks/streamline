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
import com.hortonworks.streamline.streams.cluster.register.ManualServiceRegisterer;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ZookeeperServiceRegistererTest extends AbstractServiceRegistererTest<ZookeeperServiceRegisterer> {
    public static final String ZOO_CFG = "zoo.cfg";
    public static final String ZOO_CFG_FILE_PATH = REGISTER_RESOURCE_DIRECTORY + ZOO_CFG;
    public static final String ZOO_CFG_BADCASE_FILE_PATH = REGISTER_BADCASE_RESOURCE_DIRECTORY + ZOO_CFG;
    public static final String COMPONENT_ZOOKEEPER_SERVER = "ZOOKEEPER_SERVER";

    public ZookeeperServiceRegistererTest() {
        super(ZookeeperServiceRegisterer.class);
    }

    @Before
    public void setUp() {
        resetEnvironmentService();
    }

    @Test
    public void testRegister_happyCase() throws Exception {
        Cluster cluster = getTestCluster(1L);

        ZookeeperServiceRegisterer registerer = initializeServiceRegisterer();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(ZOO_CFG_FILE_PATH)) {
            Config config = new Config();
            config.put(ZookeeperServiceRegisterer.PARAM_ZOOKEEPER_SERVER_HOSTNAMES, "zookeeper-1,zookeeper-2");
            ManualServiceRegisterer.ConfigFileInfo zooCfg = new ManualServiceRegisterer.ConfigFileInfo(ZOO_CFG, is);
            registerer.register(cluster, config, Lists.newArrayList(zooCfg));
        }
    }

    @Test
    public void testRegister_requiredPropertyNotPresented() throws Exception {
        Cluster cluster = getTestCluster(1L);

        ZookeeperServiceRegisterer registerer = initializeServiceRegisterer();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(ZOO_CFG_BADCASE_FILE_PATH)) {
            Config config = new Config();
            config.put(ZookeeperServiceRegisterer.PARAM_ZOOKEEPER_SERVER_HOSTNAMES, "zookeeper-1,zookeeper-2");
            ManualServiceRegisterer.ConfigFileInfo zooCfg = new ManualServiceRegisterer.ConfigFileInfo(ZOO_CFG, is);
            registerer.register(cluster, config, Lists.newArrayList(zooCfg));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testRegister_component_zookeeper_server_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        ZookeeperServiceRegisterer registerer = initializeServiceRegisterer();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(ZOO_CFG_FILE_PATH)) {
            ManualServiceRegisterer.ConfigFileInfo zooCfg = new ManualServiceRegisterer.ConfigFileInfo(ZOO_CFG, is);
            registerer.register(cluster, new Config(), Lists.newArrayList(zooCfg));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testRegister_zoo_cfg_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        ZookeeperServiceRegisterer registerer = initializeServiceRegisterer();

        try {
            Config config = new Config();
            config.put(ZookeeperServiceRegisterer.PARAM_ZOOKEEPER_SERVER_HOSTNAMES, "zookeeper-1,zookeeper-2");
            registerer.register(cluster, config, Lists.newArrayList());
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

}