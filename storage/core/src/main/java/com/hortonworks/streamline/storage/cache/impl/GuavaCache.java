package com.hortonworks.streamline.storage.cache.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.hortonworks.streamline.cache.Cache;
import com.hortonworks.streamline.cache.exception.NonexistentStorableKeyException;
import com.hortonworks.streamline.cache.stats.CacheStats;
import com.hortonworks.streamline.cache.view.config.ExpiryPolicy;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.StorageManager;
import com.hortonworks.streamline.storage.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by hlouro on 8/6/15.
 */
public class GuavaCache implements Cache<StorableKey, Storable> {
    private static final Logger log = LoggerFactory.getLogger(GuavaCache.class);
    private final StorageManager dao;
    private final LoadingCache<StorableKey, Storable> guavaCache;

    public GuavaCache(final StorageManager dao, CacheBuilder guavaCacheBuilder) {
        this.dao = dao;
        this.guavaCache = guavaCacheBuilder.build(new CacheLoader<StorableKey, Storable>() {
            @Override
            public Storable load(StorableKey key) throws StorageException, NonexistentStorableKeyException {
                Storable val = dao.get(key);
                if (val != null) {
                    return val;
                }
                throw new NonexistentStorableKeyException("Nonexistent key : [" + key + "]");
            }
        });
    }

    public Storable get(StorableKey key) {
        Storable val = null;
        try {
            val = guavaCache.get(key);
        } catch (UncheckedExecutionException e) {
            // we need to handle the NonexistentStorableKeyException like this because
            // the method com.google.common.cache.LocalCache.LoadingValueReference.loadFuture() (line #3544 as of guava 18)
            // catches a throwable and converts it to another form of exception
            if (e.getCause() instanceof NonexistentStorableKeyException) {
                log.debug("Failed to load nonexistent key [" + key + "]. Returning null value." + Thread.currentThread());
                return val;
            }
            throw new StorageException(e);
        } catch (ExecutionException e) {
            throw new StorageException(e);
        }
        return val;
    }

    public Map<StorableKey, Storable> getAll(Collection<? extends StorableKey> keys) {
        return guavaCache.getAllPresent(keys);
    }

    public void put(StorableKey key, Storable val) {
        guavaCache.put(key, val);
    }

    public void putAll(Map<? extends StorableKey, ? extends Storable> map) {
        guavaCache.putAll(map);
    }

    public void remove(StorableKey key) {
        guavaCache.invalidate(key);
    }

    public void removeAll(Collection<? extends StorableKey> keys) {
        final ImmutableMap<StorableKey, Storable> allPresent = guavaCache.getAllPresent(keys);
        guavaCache.invalidateAll(keys);
    }

    public void clear() {
        guavaCache.invalidateAll();
    }

    public long size() {
        return guavaCache.size();
    }

    //TODO
    public CacheStats stats() {
        return null;
    }

    @Override
    public ExpiryPolicy getExpiryPolicy() {
        return null;    //TODO
    }

    public StorageManager getDao() {
        return dao;
    }
}
