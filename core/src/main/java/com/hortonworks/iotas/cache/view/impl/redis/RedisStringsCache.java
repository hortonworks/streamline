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

package com.hortonworks.iotas.cache.view.impl.redis;

import com.hortonworks.iotas.cache.AbstractCache;
import com.hortonworks.iotas.cache.Cache;
import com.hortonworks.iotas.cache.stats.CacheStats;
import com.hortonworks.iotas.cache.view.config.ExpiryPolicy;
import com.hortonworks.iotas.storage.exception.StorageException;
import com.lambdaworks.redis.RedisConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedisStringsCache<K, V> extends RedisAbstractCache<K, V> implements Cache<K, V> {
    private static final Logger LOG = LoggerFactory.getLogger(RedisStringsCache.class);

    public RedisStringsCache(RedisConnection<K, V> redisConnection) {
        this(redisConnection, null);
    }

    public RedisStringsCache(RedisConnection<K, V> redisConnection, ExpiryPolicy expiryPolicy) {
        super(redisConnection, expiryPolicy);
    }

    @Override
    public V get(K key) throws StorageException {
        return redisConnection.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<K, V> getAll(Collection<? extends K> keys) {
        final K[] ks = keys.toArray(((K[]) new Object[keys.size()]));
        final List<V> vals = redisConnection.mget(ks);
        final HashMap<K, V> present = new HashMap<>();

        if (ks.length != vals.size()) {
            LOG.error("Number of keys [{}] does not match unexpected number of values [{}]. Returning empty map");
        } else {
            for (int i = 0; i < vals.size(); i++) { // values come in order from Redis
                final V val = vals.get(i);
                if (val != null) {
                    present.put(ks[i], val);
                } else {
                    LOG.debug("Key [{}] has null value. Skipping", ks[i]);
                }
            }
        }
        LOG.debug("Entries existing in cache [{}]. Keys non existing in cache: [{}]", present, keys.removeAll(present.keySet()));
        return present;
    }

    @Override
    public void put(K key, V val) {
        redisConnection.set(key, val);
        LOG.debug("Set (key,val) => ({},{})", key, val);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> entries) {
        redisConnection.mset(new HashMap<>(entries));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void remove(K key) {
        redisConnection.del(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void removeAll(Collection<? extends K> keys) {
        redisConnection.del(keys.toArray(((K[]) new Object[keys.size()])));
    }

    @Override
    public void clear() {
//        redisConnection.flushdb();
    }

    @Override
    public long size() {
        return -1;
//        return redisConnection.keys("*").size();  //TODO
    }

    @Override
    public CacheStats stats() {
        return null;
    }

    public static class Builder<K, V> {
        private static final long DEFAULT_MAX_BYTES = 10 * 1024 * 1024;     // 10 MBs

        public Builder() {
        }

        private long sizeBytes = DEFAULT_MAX_BYTES;
        private BytesCalculator bytesCalculator;
        private long maxSizeBytes;

        public Builder setMaxSizeBytes(long maxSizeBytes) {
            this.maxSizeBytes = maxSizeBytes;
            return this;
        }

        public Builder setMaxSizeBytesConverter(BytesCalculator bytesCalculator) {
            this.bytesCalculator = bytesCalculator;
            return this;
        }

        public Cache<K, V> build() {
            if (bytesCalculator != null) {
                LOG.debug("Setting ");

            }
            return null;    //TODO
        }
    }


    public interface BytesCalculator<T> {
        /**
         * @param object object that can be used to calculate the number of bytes that should be used to expire the cache
         * @return bytes
         */
        long computeBytes(T object);
    }


}
