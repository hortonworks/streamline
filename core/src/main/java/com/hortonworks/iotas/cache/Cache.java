package com.hortonworks.iotas.cache;


import com.google.common.collect.ImmutableMap;
import com.hortonworks.iotas.cache.stats.CacheStats;
import com.hortonworks.iotas.storage.exception.StorageException;

import java.util.Map;

/**
 * Created by hlouro on 8/6/15.
 */
public interface Cache<K, V> {
    V get(K key) throws StorageException;
    ImmutableMap<K, V> getAllPresent(Iterable<? extends K> keys);
    void put(K key, V value);
    void putAll(Map<? extends K,? extends V> m);
    void remove(K key);
    ImmutableMap<K, V> removeAllPresent(Iterable<? extends K> keys);
    void clear();
    long size();
    CacheStats stats();
}
