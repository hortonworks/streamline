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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StormServiceRegistrarTest extends AbstractServiceRegistrarTest<StormServiceRegistrar> {
    private static final String CONFIGURATION_NAME_STORM_YAML = "storm";
    private static final String CONFIGURATION_NAME_STORM_ENV = "storm-env";

    public StormServiceRegistrarTest() {
        super(StormServiceRegistrar.class);
    }

    @Before
    public void setUp() {
        resetEnvironmentService();
    }

    @Test
    public void testRegister() throws Exception {
        Cluster cluster = getTestCluster(1L);

        StormServiceRegistrar registrar = initializeServiceRegistrar();

        // explicit convert Object
        Config config = new Config();
        config.put(StormServiceRegistrar.PARAM_NIMBUS_SEEDS, "storm-1,storm-2");
        config.put(StormServiceRegistrar.PARAM_NIMBUS_THRIFT_PORT, (Object) 6627);
        config.put(StormServiceRegistrar.PARAM_UI_HOST, "storm-1");
        config.put(StormServiceRegistrar.PARAM_UI_PORT, (Object) 8080);
        config.put(StormServiceRegistrar.PARAM_NIMBUS_THRIFT_MAX_BUFFER_SIZE, (Object) 102476800);
        config.put(StormServiceRegistrar.PARAM_THRIFT_TRANSPORT, "org.apache.storm.security.auth.SimpleTransportPlugin");
        config.put(StormServiceRegistrar.PARAM_PRINCIPAL_TO_LOCAL, "org.apache.storm.security.auth.DefaultPrincipalToLocal");
        config.put(StormServiceRegistrar.PARAM_NIMBUS_PRINCIPAL_NAME, "nimbus/_HOST@EXAMPLE.COM");
        registrar.register(cluster, config, Collections.emptyList());

        Service stormService = environmentService.getServiceByName(cluster.getId(), Constants.Storm.SERVICE_NAME);
        assertNotNull(stormService);

        Component nimbus = environmentService.getComponentByName(stormService.getId(), ComponentPropertyPattern.NIMBUS.name());
        assertNotNull(nimbus);

        Collection<ComponentProcess> nimbusProcesses = environmentService.listComponentProcessesInComponent(nimbus.getId());
        List<String> hosts = nimbusProcesses.stream().map(ComponentProcess::getHost).collect(Collectors.toList());
        assertEquals(Sets.newHashSet("storm-1", "storm-2"), new HashSet<>(hosts));
        List<Integer> ports = nimbusProcesses.stream().map(ComponentProcess::getPort).collect(Collectors.toList());
        assertEquals(Sets.newHashSet(6627, 6627), new HashSet<>(ports));

        Component ui = environmentService.getComponentByName(stormService.getId(), ComponentPropertyPattern.STORM_UI_SERVER.name());
        assertNotNull(ui);

        Collection<ComponentProcess> uiProcesses = environmentService.listComponentProcessesInComponent(ui.getId());
        assertEquals(Sets.newHashSet("storm-1"), uiProcesses.stream().map(ComponentProcess::getHost).collect(Collectors.toSet()));
        assertEquals(Sets.newHashSet(8080), uiProcesses.stream().map(ComponentProcess::getPort).collect(Collectors.toSet()));

        ServiceConfiguration stormYamlConf = environmentService.getServiceConfigurationByName(stormService.getId(), CONFIGURATION_NAME_STORM_YAML);
        assertNotNull(stormYamlConf);
        Map<String, String> stormYamlConfMap = stormYamlConf.getConfigurationMap();
        assertEquals(config.getAny(StormServiceRegistrar.PARAM_NIMBUS_THRIFT_MAX_BUFFER_SIZE), Integer.valueOf(stormYamlConfMap.get(StormServiceRegistrar.PARAM_NIMBUS_THRIFT_MAX_BUFFER_SIZE)));
        assertEquals(config.get(StormServiceRegistrar.PARAM_THRIFT_TRANSPORT), stormYamlConfMap.get(StormServiceRegistrar.PARAM_THRIFT_TRANSPORT));
        assertEquals(config.get(StormServiceRegistrar.PARAM_PRINCIPAL_TO_LOCAL), stormYamlConfMap.get(StormServiceRegistrar.PARAM_PRINCIPAL_TO_LOCAL));

        ServiceConfiguration stormEnvConf = environmentService.getServiceConfigurationByName(stormService.getId(), CONFIGURATION_NAME_STORM_ENV);
        assertNotNull(stormEnvConf);
        Map<String, String> stormEnvConfMap = stormEnvConf.getConfigurationMap();
        assertEquals(config.get(StormServiceRegistrar.PARAM_NIMBUS_PRINCIPAL_NAME), stormEnvConfMap.get(StormServiceRegistrar.PARAM_NIMBUS_PRINCIPAL_NAME));
    }

    @Test
    public void testRegisterWithoutOptionalParams() throws Exception {
        Cluster cluster = getTestCluster(1L);

        StormServiceRegistrar registrar = initializeServiceRegistrar();

        Config config = new Config();
        config.put(StormServiceRegistrar.PARAM_NIMBUS_SEEDS, "storm-1,storm-2");
        config.put(StormServiceRegistrar.PARAM_NIMBUS_THRIFT_PORT, (Object) 6627);
        config.put(StormServiceRegistrar.PARAM_UI_HOST, "storm-1");
        config.put(StormServiceRegistrar.PARAM_UI_PORT, (Object) 8080);
        registrar.register(cluster, config, Collections.emptyList());

        Service stormService = environmentService.getServiceByName(cluster.getId(), Constants.Storm.SERVICE_NAME);
        assertNotNull(stormService);
        ServiceConfiguration stormYamlConf = environmentService.getServiceConfigurationByName(stormService.getId(), CONFIGURATION_NAME_STORM_YAML);
        assertNotNull(stormYamlConf);
        ServiceConfiguration stormEnvConf = environmentService.getServiceConfigurationByName(stormService.getId(), CONFIGURATION_NAME_STORM_ENV);
        assertNotNull(stormEnvConf);
        Map<String, String> confMap = stormEnvConf.getConfigurationMap();
        assertFalse(confMap.containsKey(StormServiceRegistrar.PARAM_NIMBUS_THRIFT_MAX_BUFFER_SIZE));
        assertFalse(confMap.containsKey(StormServiceRegistrar.PARAM_PRINCIPAL_TO_LOCAL));
        assertFalse(confMap.containsKey(StormServiceRegistrar.PARAM_THRIFT_TRANSPORT));
        assertFalse(confMap.containsKey(StormServiceRegistrar.PARAM_NIMBUS_PRINCIPAL_NAME));
    }

    @Test
    public void testRegister_component_storm_ui_server_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        StormServiceRegistrar registrar = initializeServiceRegistrar();

        try {
            Config config = new Config();
            config.put(StormServiceRegistrar.PARAM_NIMBUS_SEEDS, "storm-1,storm-2");
            config.put(StormServiceRegistrar.PARAM_NIMBUS_THRIFT_PORT, (Object) 6627);
            // no ui params
            registrar.register(cluster, config, Collections.emptyList());
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
            Service stormService = environmentService.getServiceByName(cluster.getId(), Constants.Storm.SERVICE_NAME);
            assertNull(stormService);
        }
    }

    @Test
    public void testRegister_component_nimbus_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        StormServiceRegistrar registrar = initializeServiceRegistrar();

        try {
            Config config = new Config();
            // no nimbus params
            config.put(StormServiceRegistrar.PARAM_UI_HOST, "storm-1");
            config.put(StormServiceRegistrar.PARAM_UI_PORT, (Object) 8080);
            registrar.register(cluster, config, Collections.emptyList());
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
            Service stormService = environmentService.getServiceByName(cluster.getId(), Constants.Storm.SERVICE_NAME);
            assertNull(stormService);
        }
    }

}