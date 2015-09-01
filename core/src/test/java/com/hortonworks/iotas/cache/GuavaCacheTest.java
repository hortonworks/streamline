package com.hortonworks.iotas.cache;

import com.google.common.cache.CacheBuilder;
import com.hortonworks.iotas.cache.impl.GuavaCache;
import com.hortonworks.iotas.catalog.Device;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.InMemoryStorageManager;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.StorageManager;
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

            final Storable putVal = new Device();
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

}
