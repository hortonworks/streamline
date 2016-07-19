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

package com.hortonworks.iotas.cache.view.service;

import com.hortonworks.iotas.cache.view.Factory;
import com.hortonworks.iotas.cache.view.config.CacheConfig;
import com.hortonworks.iotas.cache.view.config.ExpiryPolicy;
import com.hortonworks.iotas.cache.view.config.TypeConfig;
import com.hortonworks.iotas.cache.view.config.ViewConfig;
import com.hortonworks.iotas.cache.view.impl.redis.RedisHashesCache;
import com.hortonworks.iotas.cache.view.impl.redis.RedisStringsCache;
import com.lambdaworks.redis.RedisConnection;

import java.util.Arrays;
import java.util.List;

public class RedisCacheService<K,V> extends DataStoreBackedCacheService<K, V> {
    private Factory<RedisConnection<K,V>> connFactory;

    private RedisCacheService(Builder<K,V> builder) {
        super(builder);
        this.connFactory = builder.connFactory;
    }

    public static class Builder<K,V> extends DataStoreBackedCacheService.Builder<K,V> {
        private Factory<RedisConnection<K,V>> connFactory;

        public Builder(String id, TypeConfig.Cache cacheType, Factory<RedisConnection<K,V>> connFactory) {
            super(id, cacheType);
            this.connFactory = connFactory;
        }

        public RedisCacheService<K,V> build() {
            return new RedisCacheService<>(this);
        }

        public RedisCacheService<K,V> build(CacheConfig cacheConfig) {
            RedisCacheService<K, V> cacheService = new RedisCacheService<>(this);
            registerCaches(cacheService, cacheConfig);
            return cacheService;
        }

        private void registerCaches(RedisCacheService<K, V> cacheService, CacheConfig cacheConfig) {
            List<ViewConfig> viewsConfig = cacheConfig.getViewsConfig();
            for (ViewConfig viewConfig : viewsConfig) {
                registerCache(cacheService, viewConfig);
            }
        }

        private void registerCache(RedisCacheService<K, V> cacheService, ViewConfig viewConfig) {
            final ExpiryPolicy expiryPolicy = viewConfig.getExpiryPolicy();
            final String id = viewConfig.getId();
            final TypeConfig.RedisDatatype redisDatatype = ((ViewConfig.RedisViewConfig) viewConfig).getRedisDatatype();

            switch (redisDatatype) {
                case STRINGS:
                    cacheService.registerStringsCache(id, expiryPolicy);
                    break;
                case HASHES:
                    String key = ((ViewConfig.RedisViewConfig) viewConfig).getKey();
                    cacheService.registerHashesCache(id, (K) key, expiryPolicy);
                    break;
                default:
                    throw new IllegalStateException("Unsupported Redis type: " + redisDatatype
                            + ". Valid options are " + Arrays.toString(TypeConfig.RedisDatatype.values()));
            }
        }
    }

    public void registerHashesCache(String id, K key) {
        registerHashesCache(id, key, super.expiryPolicy);
    }

    public void registerHashesCache(String id, K key, ExpiryPolicy expiryPolicy) {
        registerCache(id, createRedisHashesCache(key, expiryPolicy));
    }

    public void registerStringsCache(String id) {
        registerStringsCache(id, super.expiryPolicy);
    }

    public void registerStringsCache(String id, ExpiryPolicy expiryPolicy) {
        registerCache(id, createRedisStringsCache(expiryPolicy));
    }

    public void registerDelegateCache(String id) {
        //TODO
    }

    public Factory<RedisConnection<K, V>> getConnFactory() {
        return connFactory;
    }

    private RedisHashesCache<K, V> createRedisHashesCache(K key, ExpiryPolicy expiryPolicy) {
        final ExpiryPolicy ep = expiryPolicy != null ? expiryPolicy : super.expiryPolicy;
        return new RedisHashesCache<>(connFactory.create(), key, ep);
    }

    private RedisStringsCache<K, V> createRedisStringsCache(ExpiryPolicy expiryPolicy) {
        final ExpiryPolicy ep = expiryPolicy != null ? expiryPolicy : super.expiryPolicy;
        return new RedisStringsCache<>(connFactory.create(), ep);
    }
}
