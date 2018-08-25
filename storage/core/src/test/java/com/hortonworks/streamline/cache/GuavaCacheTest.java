/**
 * Copyright 2016 Hortonworks.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.hortonworks.streamline.cache;

import com.google.common.cache.CacheBuilder;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.StorageManager;
import com.hortonworks.streamline.storage.cache.impl.GuavaCache;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;
import com.hortonworks.streamline.storage.impl.memory.InMemoryStorageManager;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class GuavaCacheTest {
    private static Cache<StorableKey, Storable> cache;
    private static StorableKey storableKey;

    @BeforeClass
    public static void init() {
        setCache();
        setStorableKey();
    }

    private static void setCache() {
        final InMemoryStorageManager dao = new InMemoryStorageManager();
        final CacheBuilder cacheBuilder = getGuavaCacheBuilder();
        cache = getCache(dao, cacheBuilder);
    }

    private static void setStorableKey() {
        Map<Schema.Field, Object> fieldsToVal = new HashMap<>();
        fieldsToVal.put(new Schema.Field("fn", Schema.Type.STRING), "fv");
        storableKey = new StorableKey("ns", new PrimaryKey(fieldsToVal));
    }

    @Test
    public void test_get_unexisting_key_null_expected() {
        Storable val = cache.get(storableKey);
        Assert.assertNull(val);
    }

    /**
     * Asserts that cache has not given key, puts (key,val) pair, retrieves key, and removes key at the end
     **/
    @Test
    public void test_put_get_existing_remove_key() {
        try {
            Assert.assertNull(cache.get(storableKey));

            final Storable putVal = new TestStorable();
            cache.put(storableKey, putVal);
            Storable retrievedVal = cache.get(storableKey);

            Assert.assertEquals(putVal, retrievedVal);
        } finally {
            cache.remove(storableKey);
            Assert.assertNull(cache.get(storableKey));
        }
    }

    private static Cache<StorableKey, Storable> getCache(StorageManager dao, CacheBuilder guavaCacheBuilder) {
        return new GuavaCache(dao, guavaCacheBuilder);
    }

    private static CacheBuilder getGuavaCacheBuilder() {
        final long maxSize = 1000;
        return CacheBuilder.newBuilder().maximumSize(maxSize);
    }

    private class TestStorable extends AbstractStorable {
        private static final String NAMESPACE = "test";

        private Long id;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        @Override
        public String getNameSpace() {
            return NAMESPACE;
        }

        @Override
        public PrimaryKey getPrimaryKey() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestStorable that = (TestStorable) o;

            return id != null ? id.equals(that.id) : that.id == null;

        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }
    }

    }
