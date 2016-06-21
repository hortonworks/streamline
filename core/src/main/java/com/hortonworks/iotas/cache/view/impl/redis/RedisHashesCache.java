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
import java.util.Map;

@SuppressWarnings("unchecked")
public class RedisHashesCache<K, V> extends RedisAbstractCache<K, V> implements Cache<K, V> {
    private static   final Logger LOG = LoggerFactory.getLogger(RedisHashesCache.class);

    private K key;

    public RedisHashesCache(RedisConnection<K, V> redisConnection, K key) {
        this(redisConnection, key, null);
    }

    public RedisHashesCache(RedisConnection<K, V> redisConnection, K key, ExpiryPolicy expiryPolicy) {
        super(redisConnection, expiryPolicy);
        this.key = key;
    }

    @Override
    public V get(K field) throws StorageException {
        return redisConnection.hget(key, field);
    }

    @Override
    public Map<K, V> getAll(Collection<? extends K> fields) {
        Map<K, V> present = redisConnection.hgetall(key);
        present.entrySet().retainAll(fields);
        LOG.debug("Entries existing in cache [{}]. Keys non existing in cache: [{}]", present, fields.removeAll(present.keySet()));
        return present;
    }

    @Override
    public void put(K field, V val) {
        redisConnection.hset(key, field, val);
        LOG.debug("Set (key, field, val) => ({},{})", key, field, val);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> entries) {
        redisConnection.hmset(key, new HashMap<>(entries));
    }

    @Override
    public void remove(K field) {
        redisConnection.hdel(key, field);
    }

    @Override
    public void removeAll(Collection<? extends K> fields) {
        redisConnection.del(fields.toArray(((K[]) new Object[fields.size()])));
    }

    @Override
    public void clear() {
        redisConnection.del(key);
    }

    @Override
    public long size() {
        return redisConnection.hlen(key);
    }

    @Override
    public CacheStats stats() {
        return null;
    }
}
