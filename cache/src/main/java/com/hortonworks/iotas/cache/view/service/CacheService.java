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

import com.hortonworks.iotas.cache.Cache;
import com.hortonworks.iotas.cache.view.config.ExpiryPolicy;
import com.hortonworks.iotas.cache.view.config.TypeConfig;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CacheService<K,V> {
    protected final ConcurrentMap<String, Cache<K,V>> caches = new ConcurrentHashMap<>();

    protected final String id;
    protected final TypeConfig.Cache cacheType;
    protected ExpiryPolicy expiryPolicy;  // ExpiryPolicy used by all the caches registered in this service, if not overridden for a particular cache

    public CacheService(String id, TypeConfig.Cache cacheType) {
        this.id = id;
        this.cacheType = cacheType;
    }

    protected CacheService(Builder<K,V> builder) {
        this.id = builder.id;
        this.cacheType = builder.cacheType;
        this.expiryPolicy= builder.expiryPolicy;
    }

    public static class Builder<K,V> {
        private final String id;
        private final TypeConfig.Cache cacheType;
        private ExpiryPolicy expiryPolicy;

        public Builder(String id, TypeConfig.Cache cacheType) {
            this.id = id;
            this.cacheType= cacheType;
        }

        /**
         * Sets the {@link ExpiryPolicy} used by all the caches registered, if not overridden for a particular cache
         */
        public Builder<K,V> setExpiryPolicy(ExpiryPolicy expiryPolicy) {
            this.expiryPolicy = expiryPolicy;
            return this;
        }

        public CacheService<K,V> build() {
            return new CacheService<>(this);
        }
    }

    public <T extends Cache<K,V>> T getCache(String namespace) {
        return (T) caches.get(namespace);
    }

    public void registerCache(String id, Cache<K,V> cache) {
        caches.putIfAbsent(id, cache);   //TODO. What to do when attempting to register a cache with an ID that already exists?
    }

    public String getServiceId() {
        return id;
    }

    public TypeConfig.Cache getCacheType() {
        return cacheType;
    }

    public Set<String> getCacheIds() {
        return caches.keySet();
    }

    public ExpiryPolicy getExpiryPolicy() {
        return expiryPolicy;
    }
}
