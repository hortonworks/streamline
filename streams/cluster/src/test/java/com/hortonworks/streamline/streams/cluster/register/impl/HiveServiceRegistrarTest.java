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
import com.hortonworks.streamline.streams.cluster.register.ManualServiceRegistrar;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;

import static org.junit.Assert.fail;

@RunWith(JMockit.class)
public class HiveServiceRegistrarTest extends AbstractServiceRegistrarTest<HiveServiceRegistrar> {
    public static final String HIVE_SITE_XML = "hive-site.xml";
    public static final String HIVE_SITE_XML_FILE_PATH = REGISTER_RESOURCE_DIRECTORY + HIVE_SITE_XML;
    public static final String HIVE_SITE_XML_BADCASE_FILE_PATH = REGISTER_BADCASE_RESOURCE_DIRECTORY + HIVE_SITE_XML;

    public HiveServiceRegistrarTest() {
        super(HiveServiceRegistrar.class);
    }

    @Before
    public void setUp() {
        resetEnvironmentService();
    }

    @Test
    public void testRegister() throws Exception {
        Cluster cluster = getTestCluster(1L);

        HiveServiceRegistrar registrar = initializeServiceRegistrar();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(HIVE_SITE_XML_FILE_PATH)) {
            ManualServiceRegistrar.ConfigFileInfo hiveSiteXml = new ManualServiceRegistrar.ConfigFileInfo(HIVE_SITE_XML, is);
            registrar.register(cluster, new Config(), Lists.newArrayList(hiveSiteXml));
        }
    }

    @Test
    public void testRegister_requiredPropertyNotPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        HiveServiceRegistrar registrar = initializeServiceRegistrar();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(HIVE_SITE_XML_BADCASE_FILE_PATH)) {
            ManualServiceRegistrar.ConfigFileInfo hiveSiteXml = new ManualServiceRegistrar.ConfigFileInfo(HIVE_SITE_XML, is);
            registrar.register(cluster, new Config(), Lists.newArrayList(hiveSiteXml));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testRegister_hive_site_xml_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        HiveServiceRegistrar registrar = initializeServiceRegistrar();

        try {
            registrar.register(cluster, new Config(), Lists.newArrayList());
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }
}