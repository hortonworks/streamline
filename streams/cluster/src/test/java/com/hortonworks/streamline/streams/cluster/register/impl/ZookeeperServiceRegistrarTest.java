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
import com.google.common.collect.Sets;
import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.catalog.Service;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ZookeeperServiceRegistrarTest extends AbstractServiceRegistrarTest<ZookeeperServiceRegistrar> {
    private static final String CONFIGURATION_NAME_ZOO_CFG = "zoo.cfg";

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

        Config config = new Config();
        config.put(ZookeeperServiceRegistrar.PARAM_ZOOKEEPER_SERVER_HOSTNAMES, Lists.newArrayList("zookeeper-1", "zookeeper-2"));
        config.put(ZookeeperServiceRegistrar.PARAM_ZOOKEEPER_PORT, (Object) 2181);
        registrar.register(cluster, config, Collections.emptyList());

        Service zkService = environmentService.getServiceByName(cluster.getId(), Constants.Zookeeper.SERVICE_NAME);
        assertNotNull(zkService);

        Component zkServer = environmentService.getComponentByName(zkService.getId(), ComponentPropertyPattern.ZOOKEEPER_SERVER.name());
        assertNotNull(zkServer);

        Collection<ComponentProcess> zkServerProcesses = environmentService.listComponentProcesses(zkServer.getId());
        assertEquals(Sets.newHashSet("zookeeper-1", "zookeeper-2"),
                zkServerProcesses.stream().map(ComponentProcess::getHost).collect(Collectors.toSet()));
        assertEquals(Sets.newHashSet(2181, 2181),
                zkServerProcesses.stream().map(ComponentProcess::getPort).collect(Collectors.toSet()));

        ServiceConfiguration zooConf = environmentService.getServiceConfigurationByName(zkService.getId(), CONFIGURATION_NAME_ZOO_CFG);
        assertNotNull(zooConf);
    }

    @Test
    public void testRegisterZookeeperServerPropertyNotPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        ZookeeperServiceRegistrar registrar = initializeServiceRegistrar();

        try {
            registrar.register(cluster, new Config(), Collections.emptyList());
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
            Service zkService = environmentService.getServiceByName(cluster.getId(), Constants.Zookeeper.SERVICE_NAME);
            assertNull(zkService);
        }
    }
}