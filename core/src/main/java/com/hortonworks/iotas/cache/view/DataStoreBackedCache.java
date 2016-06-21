/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.hortonworks.iotas.cache.view;

import com.hortonworks.iotas.cache.AbstractCache;
import com.hortonworks.iotas.cache.Cache;
import com.hortonworks.iotas.cache.LoadableCache;
import com.hortonworks.iotas.cache.stats.CacheStats;
import com.hortonworks.iotas.cache.view.datastore.DataStoreReader;
import com.hortonworks.iotas.cache.view.datastore.DataStoreWriter;
import com.hortonworks.iotas.cache.view.io.loader.CacheLoader;
import com.hortonworks.iotas.cache.view.io.loader.CacheLoaderCallback;
import com.hortonworks.iotas.cache.view.io.writer.CacheWriter;
import com.hortonworks.iotas.storage.exception.StorageException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DataStoreBackedCache<K,V> extends AbstractCache<K,V> implements LoadableCache<K,V> {
    private static final Logger LOG = LoggerFactory.getLogger(DataStoreBackedCache.class);

    private final Cache<K, V> cache;
    private final CacheLoader<K, V> cacheLoader;
    private final CacheWriter<K, V> cacheWriter;
    private final DataStoreReader<K, V> dataStoreReader;

    public DataStoreBackedCache(Cache<K, V> cache, CacheLoader<K, V> cacheLoader, DataStoreReader<K, V> dataStoreReader,
                                CacheWriter<K, V> cacheWriter) {

        validateArguments(cache, dataStoreReader, cacheLoader, cacheWriter);

        this.cache = cache;
        this.dataStoreReader = dataStoreReader;
        this.cacheLoader = cacheLoader;
        this.cacheWriter = cacheWriter;
    }

    public void loadAll(Collection<? extends K> keys, CacheLoaderCallback<K,V> callback) {
        if (cacheLoader != null) {
            cacheLoader.loadAll(keys, callback);
        }
    }

    @Override
    public V get(K key) throws StorageException {
        V val = cache.get(key);
        if (val == null && dataStoreReader != null) {     // in sync read through
            val = dataStoreReader.read(key);
        }
        return val;
    }

    @Override
    public void put(K key, V val) {
        cache.put(key, val);
        if (cacheWriter != null) {              // in sync write through
            cacheWriter.write(key, val);
        }
    }

    @Override
    public Map<K, V> getAll(Collection<? extends K> keys) {  // TODO what if trying to load more keys than max number of keys that be kept in the cache ?
        Map<K, V> present = cache.getAll(keys);
        if (dataStoreReader != null) {
            if (present == null || present.isEmpty()) {
                present = dataStoreReader.readAll(keys);                  // in sync read through
            } else if (present.size() < keys.size()) {
                Set<K> notPresent = new HashSet<>(keys);
                notPresent.removeAll(present.keySet());
                Map<K, V> loaded = dataStoreReader.readAll(notPresent);   // in sync read through
                present.putAll(loaded);
            }
        }
//        LOG.debug("Entries existing in cache [{}]. Keys non existing in cache: [{}]", present, notPresent.removeAll(loaded));
        return present;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> entries) {
        cache.putAll(entries);
        if (cacheWriter != null) {      // sync or async write, depending on the writing strategy chosen
            cacheWriter.writeAll(entries);
        }
    }

    @Override
    public void remove(K key) {
        cache.remove(key);
        if (cacheWriter != null) {      // sync or async delete, depending on the writing strategy chosen
            cacheWriter.delete(key);
        }
    }

    @Override
    public void removeAll(Collection<? extends K> keys) {
        cache.removeAll(keys);
        if (cacheWriter != null) {      // sync or async delete, depending on the writing strategy chosen
            cacheWriter.deleteAll(keys);
        }
    }

    @Override
    public void clear() {
        cache.clear();
        LOG.info("Cache cleared. Entries only removed from cache but not from backing data store");    //TODO: Do we want to remove from DB as well ?
    }

    @Override
    public long size() {
        return cache.size();
    }

    @Override
    public CacheStats stats() {
        return cache.stats();
    }

    @Override
    public String toString() {
        return "DataStoreBackedCache{" +
                "cache=" + cache +
                ", cacheLoader=" + cacheLoader +
                ", cacheWriter=" + cacheWriter +
                ", dataStore=" + dataStoreReader +
                "} " + super.toString();
    }

    // =========== Private helper methods ===========

    private void validateArguments(Cache<K, V> cache, DataStoreReader<K, V> dataStore,
                                   CacheLoader<K, V> cacheLoader, DataStoreWriter<K, V> dataStoreWriter) {
        if (cache == null) {
            throw new IllegalArgumentException("Cache reference cannot be null");
        }

        if (dataStore == null && cacheLoader == null && dataStoreWriter == null) {
            throw new IllegalArgumentException(String.format("At least one non null implementation of %s,  %s, or  %s " +
                            "is required. If no backing data store is required consider using a non backed implementation of %s",
                    getSimpleName(DataStoreReader.class), getSimpleName(DataStoreWriter.class),
                    getSimpleName(CacheLoader.class), getSimpleName(Cache.class)));
        }

        LOG.info("Created {}", this);
    }

    private String getSimpleName(Class<?> clazz) {
        return clazz.getSimpleName();
    }

}
