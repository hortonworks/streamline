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
import com.hortonworks.iotas.cache.view.config.ExpiryPolicy;
import com.lambdaworks.redis.RedisConnection;

import java.util.Collection;

public abstract class RedisAbstractCache<K, V> extends AbstractCache<K, V> implements Cache<K, V> {
    public static final String REDIS_MAX_MEMORY = "maxmemory";
    public static final String REDIS_MAX_MEMORY_POLICY = "maxmemory-policy";
    public static final String REDIS_MAX_MEMORY_POLICY_ALL_KEYS_LRU = "allkeys-lru";

    protected final RedisConnection<K, V> redisConnection;

    public RedisAbstractCache(RedisConnection<K, V> redisConnection) {
        this(redisConnection, null);
    }

    public RedisAbstractCache(RedisConnection<K, V> redisConnection, ExpiryPolicy expiryPolicy) {
        super(expiryPolicy);
        this.redisConnection = redisConnection;
        setMaxSize();
    }

    protected void setMaxSize() {
        if (expiryPolicy.isSize()) {
            redisConnection.configSet(REDIS_MAX_MEMORY, String.valueOf(expiryPolicy.getSize().getBytes()));
            redisConnection.configSet(REDIS_MAX_MEMORY_POLICY, REDIS_MAX_MEMORY_POLICY_ALL_KEYS_LRU);
        }
    }

    // TODO
    protected void setExpiryPolicy(K key) {
        if (expiryPolicy.isTtl()) {
            redisConnection.expire(key, expiryPolicy.getTtl().getTtlSeconds());
        }
    }

    protected void setExpiryPolicy(Collection<? extends K> keys) {
        if (expiryPolicy.isTtl()) {
            for (K key : keys) {
                redisConnection.expire(key, expiryPolicy.getTtl().getTtlSeconds());
            }
        }
    }
}