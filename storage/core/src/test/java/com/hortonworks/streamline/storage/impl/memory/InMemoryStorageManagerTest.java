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
package com.hortonworks.streamline.storage.impl.memory;

import com.hortonworks.streamline.storage.AbstractStoreManagerTest;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.StorageManager;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;

public abstract class InMemoryStorageManagerTest extends AbstractStoreManagerTest {
    private final StorageManager storageManager = new InMemoryStorageManager();

    @Override
    public StorageManager getStorageManager() {
        return storageManager;
    }

    @Test
    public void testList_NonexistentNameSpace_StorageException() {
        Collection<Storable> found = getStorageManager().list("NONEXISTENT_NAME_SPACE");
        Assert.assertTrue(found.isEmpty());
    }

}
