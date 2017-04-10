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
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@RunWith(JMockit.class)
public class DruidServiceRegistrarTest extends AbstractServiceRegistrarTest<DruidServiceRegistrar> {
    public static final String DRUID_COMMON_RUNTIME_PROPERTIES = "common.runtime.properties";
    public static final String DRUID_COMMON_RUNTIME_PROPERTIES_FILE_PATH = REGISTER_RESOURCE_DIRECTORY + DRUID_COMMON_RUNTIME_PROPERTIES;
    public static final String DRUID_COMMON_RUNTIME_PROPERTIES_BADCASE_FILE_PATH = REGISTER_BADCASE_RESOURCE_DIRECTORY + DRUID_COMMON_RUNTIME_PROPERTIES;
    private static final String CONFIGURATION_NAME_COMMON_RUNTIME_PROPERTIES = "common.runtime";

    public DruidServiceRegistrarTest() {
        super(DruidServiceRegistrar.class);
    }

    @Before
    public void setUp() {
        resetEnvironmentService();
    }

    @Test
    public void testRegister_happyCase() throws Exception {
        Cluster cluster = getTestCluster(1L);

        DruidServiceRegistrar registerer = initializeServiceRegistrar();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(DRUID_COMMON_RUNTIME_PROPERTIES_FILE_PATH)) {
            ManualServiceRegistrar.ConfigFileInfo hiveSiteXml = new ManualServiceRegistrar.ConfigFileInfo(DRUID_COMMON_RUNTIME_PROPERTIES, is);
            registerer.register(cluster, new Config(), Lists.newArrayList(hiveSiteXml));
        }

        Service druidService = environmentService.getServiceByName(cluster.getId(), Constants.Druid.SERVICE_NAME);
        assertNotNull(druidService);
        ServiceConfiguration commonRuntimePropertiesConf = environmentService.getServiceConfigurationByName(druidService.getId(), CONFIGURATION_NAME_COMMON_RUNTIME_PROPERTIES);
        assertNotNull(commonRuntimePropertiesConf);
    }

    @Test
    public void testRegister_requiredPropertyNotPresented() throws Exception {
        Cluster cluster = getTestCluster(1L);

        DruidServiceRegistrar registerer = initializeServiceRegistrar();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(DRUID_COMMON_RUNTIME_PROPERTIES_BADCASE_FILE_PATH)) {
            ManualServiceRegistrar.ConfigFileInfo hiveSiteXml = new ManualServiceRegistrar.ConfigFileInfo(DRUID_COMMON_RUNTIME_PROPERTIES, is);
            registerer.register(cluster, new Config(), Lists.newArrayList(hiveSiteXml));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
            Service druidService = environmentService.getServiceByName(cluster.getId(), Constants.Druid.SERVICE_NAME);
            assertNull(druidService);
        }
    }

    @Test
    public void testRegister_common_runtime_properties_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        DruidServiceRegistrar registerer = initializeServiceRegistrar();

        try {
            registerer.register(cluster, new Config(), Lists.newArrayList());
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
            Service druidService = environmentService.getServiceByName(cluster.getId(), Constants.Druid.SERVICE_NAME);
            assertNull(druidService);
        }
    }
}