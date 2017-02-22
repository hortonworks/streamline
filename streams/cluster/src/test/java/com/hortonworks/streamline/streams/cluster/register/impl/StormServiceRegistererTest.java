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
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StormServiceRegistererTest extends AbstractServiceRegistererTest<StormServiceRegisterer> {
    public static final String STORM_YAML = "storm.yaml";
    public static final String STORM_YAML_FILE_PATH = REGISTER_RESOURCE_DIRECTORY + STORM_YAML;
    public static final String STORM_YAML_BADCASE_FILE_PATH = REGISTER_BADCASE_RESOURCE_DIRECTORY + STORM_YAML;
    public static final String COMPONENT_NIMBUS = "NIMBUS";
    public static final String COMPONENT_STORM_UI_SERVER = "STORM_UI_SERVER";

    public StormServiceRegistererTest() {
        super(StormServiceRegisterer.class);
    }

    @Before
    public void setUp() {
        resetEnvironmentService();
    }

    @Test
    public void testRegister_happyCase() throws Exception {
        Cluster cluster = getTestCluster(1L);

        StormServiceRegisterer registerer = initializeServiceRegisterer();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(STORM_YAML_FILE_PATH)) {
            Config config = new Config();
            config.put(StormServiceRegisterer.PARAM_STORM_UI_SERVER_HOSTNAME, "storm-1");
            config.put(StormServiceRegisterer.PARAM_NIMBUS_HOSTNAMES, "storm-1,storm-2");
            ManualServiceRegisterer.ConfigFileInfo stormYaml = new ManualServiceRegisterer.ConfigFileInfo(STORM_YAML, is);
            registerer.register(cluster, config, Lists.newArrayList(stormYaml));
        }
    }

    @Test
    public void testRegister_requiredPropertyNotPresented() throws Exception {
        Cluster cluster = getTestCluster(1L);

        StormServiceRegisterer registerer = initializeServiceRegisterer();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(STORM_YAML_BADCASE_FILE_PATH)) {
            Config config = new Config();
            config.put(StormServiceRegisterer.PARAM_STORM_UI_SERVER_HOSTNAME, "storm-1");
            config.put(StormServiceRegisterer.PARAM_NIMBUS_HOSTNAMES, "storm-1,storm-2");
            ManualServiceRegisterer.ConfigFileInfo stormYaml = new ManualServiceRegisterer.ConfigFileInfo(STORM_YAML, is);
            registerer.register(cluster, config, Lists.newArrayList(stormYaml));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testRegister_component_storm_ui_server_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        StormServiceRegisterer registerer = initializeServiceRegisterer();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(STORM_YAML_FILE_PATH)) {
            Config config = new Config();
            config.put(StormServiceRegisterer.PARAM_NIMBUS_HOSTNAMES, "storm-1,storm-2");
            ManualServiceRegisterer.ConfigFileInfo stormYaml = new ManualServiceRegisterer.ConfigFileInfo(STORM_YAML, is);
            registerer.register(cluster, config, Lists.newArrayList(stormYaml));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testRegister_component_nimbus_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        StormServiceRegisterer registerer = initializeServiceRegisterer();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(STORM_YAML_FILE_PATH)) {
            Config config = new Config();
            config.put(StormServiceRegisterer.PARAM_STORM_UI_SERVER_HOSTNAME, "storm-1");
            ManualServiceRegisterer.ConfigFileInfo stormYaml = new ManualServiceRegisterer.ConfigFileInfo(STORM_YAML, is);
            registerer.register(cluster, config, Lists.newArrayList(stormYaml));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testRegister_storm_yaml_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        StormServiceRegisterer registerer = initializeServiceRegisterer();

        try {
            Config config = new Config();
            config.put(StormServiceRegisterer.PARAM_STORM_UI_SERVER_HOSTNAME, "storm-1");
            config.put(StormServiceRegisterer.PARAM_NIMBUS_HOSTNAMES, "storm-1,storm-2");
            registerer.register(cluster, config, Lists.newArrayList());
        } catch (IllegalArgumentException e) {
            // OK
        }
    }
}