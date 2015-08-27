package com.hortonworks.iotas.cache.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.hortonworks.iotas.cache.Cache;
import com.hortonworks.iotas.cache.stats.CacheStats;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.StorageException;
import com.hortonworks.iotas.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            public Storable load(StorableKey key) throws StorageException {
                return dao.get(key);
            }
        });
    }

    public Storable get(StorableKey key) throws StorageException {
        try {
            return guavaCache.get(key);
        } catch (ExecutionException e) {
            throw new StorageException(e);
        }
    }

    public ImmutableMap<StorableKey, Storable> getAllPresent(Iterable<? extends StorableKey> keys) {
        return guavaCache.getAllPresent(keys);
    }

    public void put(StorableKey key, Storable value) {
        guavaCache.put(key, value);
    }

    public void putAll(Map<? extends StorableKey, ? extends Storable> map) {
        guavaCache.putAll(map);
    }

    public void remove(StorableKey key) {
        guavaCache.invalidate(key);
    }

    public ImmutableMap<StorableKey, Storable> removeAllPresent(Iterable<? extends StorableKey> keys) {
        final ImmutableMap<StorableKey, Storable> allPresent = guavaCache.getAllPresent(keys);
        guavaCache.invalidateAll(keys);
        return allPresent;
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

    public StorageManager getDao() {
        return dao;
    }
}
