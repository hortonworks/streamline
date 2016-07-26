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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hortonworks.iotas.storage.atlas;

import com.hortonworks.iotas.common.test.IntegrationTest;
import com.hortonworks.iotas.storage.AbstractStoreManagerTest;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableTest;
import com.hortonworks.iotas.storage.StorageManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static com.hortonworks.iotas.storage.atlas.AtlasMetadataServiceTest.cleanupGraphDbDir;

/**
 *
 */
@Category(IntegrationTest.class)
public class AtlasStorageManagerTest extends AbstractStoreManagerTest {
    private static AtlasStorageManager atlasStorageManager;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // cleanup existing graphdb dir
        cleanupGraphDbDir();
        atlasStorageManager = new AtlasStorageManager();
        atlasStorageManager.init(null);
        atlasStorageManager.registerStorables(Collections.<Class<? extends Storable>>singletonList(DeviceInfo.class));
    }

    @Override
    protected void setStorableTests() {
        storableTests = new ArrayList<>();
        storableTests.add(new StorableTest() {
                              {
                                  storableList = new ArrayList<Storable>() {
                                      {
                                          long x = System.currentTimeMillis();
                                          int version = 0;
                                          add(createDeviceInfo(x, "" + version));
                                          add(createDeviceInfo(x, "deviceinfo-" + (version + 1)));
                                          add(createDeviceInfo(++x, "" + version));
                                          add(createDeviceInfo(++x, "" + version));
                                      }
                                  };
                              }

                              private Storable createDeviceInfo(long id, String version) {
                                  DeviceInfo deviceInfo = new DeviceInfo();
                                  deviceInfo.setXid("" + id);
                                  deviceInfo.setName("deviceinfo-" + id);
                                  deviceInfo.setTimestamp("" + System.currentTimeMillis());
                                  deviceInfo.setVersion(version);
                                  return deviceInfo;
                              }
                          }
        );
    }

    @Override
    protected StorageManager getStorageManager() {
        return atlasStorageManager;
    }


    @Before
    public void setup() {
        super.setup();
        Collection<Storable> storables = getStorageManager().list(DeviceInfo.NAME_SPACE);
        for (Storable storable : storables) {
            getStorageManager().remove(storable.getStorableKey());
        }
    }

    @Test
    public void testNextId_AutoincrementColumn_IdPlusOne() throws Exception {
        for (StorableTest storableTest : storableTests) {
            Long currentId = getStorageManager().nextId(storableTest.getNameSpace());
            for (int i = 0; i < 10; i++) {
                Long nextId = getStorageManager().nextId(storableTest.getNameSpace());
                Assert.assertTrue(currentId < nextId);
                currentId = nextId;
            }
        }
    }
}
