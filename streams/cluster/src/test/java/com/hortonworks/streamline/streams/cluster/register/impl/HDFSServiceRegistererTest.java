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

import static org.junit.Assert.*;

public class HDFSServiceRegistererTest extends AbstractServiceRegistererTest<HDFSServiceRegisterer> {
    public static final String CORE_SITE_XML = "core-site.xml";
    public static final String HDFS_SITE_XML = "hdfs-site.xml";
    public static final String CORE_SITE_XML_FILE_PATH = REGISTER_RESOURCE_DIRECTORY + CORE_SITE_XML;
    public static final String HDFS_SITE_XML_FILE_PATH = REGISTER_RESOURCE_DIRECTORY + HDFS_SITE_XML;
    public static final String CORE_SITE_XML_BADCASE_FILE_PATH = REGISTER_BADCASE_RESOURCE_DIRECTORY + CORE_SITE_XML;
    public static final String HDFS_SITE_XML_BADCASE_FILE_PATH = REGISTER_BADCASE_RESOURCE_DIRECTORY + HDFS_SITE_XML;

    public HDFSServiceRegistererTest() {
        super(HDFSServiceRegisterer.class);
    }

    @Before
    public void setUp() {
        resetEnvironmentService();
    }

    @Test
    public void testRegister_happyCase() throws Exception {
        Cluster cluster = getTestCluster(1L);

        HDFSServiceRegisterer registerer = initializeServiceRegisterer();

        try (InputStream coreSiteIs = getClass().getClassLoader().getResourceAsStream(CORE_SITE_XML_FILE_PATH);
             InputStream hdfsSiteIs = getClass().getClassLoader().getResourceAsStream(HDFS_SITE_XML_FILE_PATH)) {
            ManualServiceRegisterer.ConfigFileInfo coreSiteXml = new ManualServiceRegisterer.ConfigFileInfo(CORE_SITE_XML, coreSiteIs);
            ManualServiceRegisterer.ConfigFileInfo hdfsSiteXml = new ManualServiceRegisterer.ConfigFileInfo(HDFS_SITE_XML, hdfsSiteIs);
            registerer.register(cluster, new Config(), Lists.newArrayList(coreSiteXml, hdfsSiteXml));
        }
    }

    @Test
    public void testRegister_requiredPropertyNotPresented() throws Exception {
        Cluster cluster = getTestCluster(1L);

        HDFSServiceRegisterer registerer = initializeServiceRegisterer();

        try (InputStream coreSiteIs = getClass().getClassLoader().getResourceAsStream(CORE_SITE_XML_BADCASE_FILE_PATH);
             InputStream hdfsSiteIs = getClass().getClassLoader().getResourceAsStream(HDFS_SITE_XML_BADCASE_FILE_PATH)) {
            ManualServiceRegisterer.ConfigFileInfo coreSiteXml = new ManualServiceRegisterer.ConfigFileInfo(CORE_SITE_XML, coreSiteIs);
            ManualServiceRegisterer.ConfigFileInfo hdfsSiteXml = new ManualServiceRegisterer.ConfigFileInfo(HDFS_SITE_XML, hdfsSiteIs);
            registerer.register(cluster, new Config(), Lists.newArrayList(coreSiteXml, hdfsSiteXml));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testRegister_hdfs_site_xml_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        HDFSServiceRegisterer registerer = initializeServiceRegisterer();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CORE_SITE_XML_FILE_PATH)) {
            ManualServiceRegisterer.ConfigFileInfo coreSiteXml = new ManualServiceRegisterer.ConfigFileInfo(CORE_SITE_XML, is);
            registerer.register(cluster, new Config(), Lists.newArrayList(coreSiteXml));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    @Test
    public void testRegister_core_site_xml_notPresent() throws Exception {
        Cluster cluster = getTestCluster(1L);

        HDFSServiceRegisterer registerer = initializeServiceRegisterer();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(HDFS_SITE_XML_FILE_PATH)) {
            ManualServiceRegisterer.ConfigFileInfo hdfsSiteXml = new ManualServiceRegisterer.ConfigFileInfo(HDFS_SITE_XML, is);
            registerer.register(cluster, new Config(), Lists.newArrayList(hdfsSiteXml));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }
}