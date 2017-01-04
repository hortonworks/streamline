/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.streamline.streams.layout.storm;

import org.apache.streamline.streams.StreamlineEvent;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HdfsSpoutFluxComponentTest {
    private static final String hdfsURI = "hdfs://namenode";;
    private static final String readerType = "text";
    private static final String sourceDir = "/tmp/source";
    private static final String archiveDir = "/tmp/done";
    private static final String badDir = "/tmp/bad";
    private static final String lockDir = "/tmp/lock";
    private static final Integer lockTimeout = 10;
//    private static final String outputfields = StreamlineEvent.STREAMLINE_EVENT;

    HdfsSpoutFluxComponent comp;
    HashMap<String, Object> conf = new HashMap<>();

    public HdfsSpoutFluxComponentTest() {
        comp = new HdfsSpoutFluxComponent();
        comp.withCatalogRootUrl("localhost:8080/api/v1/catalog");
        conf.put(HdfsSpoutFluxComponent.KEY_HDFS_URI, hdfsURI);
        conf.put(HdfsSpoutFluxComponent.KEY_READER_TYPE, readerType);
        conf.put(HdfsSpoutFluxComponent.KEY_SOURCE_DIR, sourceDir);
        conf.put(HdfsSpoutFluxComponent.KEY_ARCHIVE_DIR, archiveDir);
        conf.put(HdfsSpoutFluxComponent.KEY_BAD_FILES_DIR, badDir);
        conf.put(HdfsSpoutFluxComponent.KEY_LOCK_DIR, lockDir);
        conf.put(HdfsSpoutFluxComponent.KEY_LOCK_TIMEOUT_SEC, lockTimeout);
        comp.withConfig(conf);

    }

    @Test
    public void testGetComponent() {
        Map<String, Object> info = comp.getComponent();
        Object compId = info.get("id");
        Assert.assertNotNull( compId );
        Assert.assertNotNull( compId.toString().startsWith("hdfsSpout") );

        Object className = info.get("className");
        Assert.assertEquals("org.apache.storm.hdfs.spout.HdfsSpout", className);
        Assert.assertEquals(8, ((ArrayList)info.get("configMethods")).size() ) ;


        ArrayList configMethods = (ArrayList) info.get("configMethods");
        checkSettings(configMethods, "setHdfsUri", hdfsURI);
        checkSettings(configMethods, "setReaderType", readerType);
        checkSettings(configMethods, "setSourceDir", sourceDir);
        checkSettings(configMethods, "setArchiveDir", archiveDir);
        checkSettings(configMethods, "setBadFilesDir", badDir);
        checkSettings(configMethods, "setLockDir", lockDir);
        checkSettings(configMethods, "setLockTimeoutSec", lockTimeout.toString());
    }

    private void checkSettings(ArrayList configMethods, String setting, String expectedVal) {
        for (int i = 0; i < configMethods.size(); i++) {
            String key = ((Map)configMethods.get(i)).get("name").toString();
            if(key.equalsIgnoreCase(setting)) {
                String value = ((ArrayList) ((Map) configMethods.get(i)).get("args")).get(0).toString();
                Assert.assertEquals(expectedVal, value);
            }
        }
    }

    @Test
    public void testGetReferencedComponents() {
        List<Map<String, Object>> info = comp.getReferencedComponents();
        Assert.assertTrue(info.isEmpty());
    }
}