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

package com.hortonworks.iotas.cache.view.test;

import com.hortonworks.iotas.cache.Cache;
import com.hortonworks.iotas.cache.view.DataStoreBackedCache;
import com.hortonworks.iotas.cache.view.service.CacheService;
import com.hortonworks.iotas.cache.view.service.CacheServiceId;
import com.hortonworks.iotas.cache.view.service.RedisCacheService;
import com.hortonworks.iotas.cache.view.service.registry.CacheServiceLocalRegistry;
import com.hortonworks.iotas.cache.view.service.registry.CacheServiceRegistry;

import java.util.HashMap;

public class CacheClientMain {
    private static final CacheServiceRegistry cacheRegistry = CacheServiceLocalRegistry.INSTANCE;

    public static void main(String[] args) {
        CacheClientMain instance = new CacheClientMain();
        instance.<String, String>registerCache(new CacheServiceId("id1"));
        instance.<Integer, Integer>registerCache(new CacheServiceId("id2"));
        CacheService<String, String> id1 = cacheRegistry.getCacheService(new CacheServiceId("id1"));
    }

    public void method() {
        CacheService<String, String> cacheService = cacheRegistry.getCacheService(getDefaultRedisId());
        Cache<String, String> cache = cacheService.getCache("namespace");
        String val = cache.get("key");
        cache.put("key", "val");
        cache.putAll(new HashMap<String, String>(){{put("key", "val"); put("key1", "val1");}});
    }

    public void method1() {
        RedisCacheService cacheService = (RedisCacheService) cacheRegistry.<String, String>getCacheService(getDefaultRedisId());
        DataStoreBackedCache<String, String> cache = (DataStoreBackedCache<String, String>) cacheService.getCache("");
        String val = cache.get("key");
        cache.put("key", "val");
        cache.putAll(new HashMap<String, String>(){{put("key", "val"); put("key1", "val1");}});
    }

    private CacheServiceId getDefaultRedisId() {
        return getRedisId("localhost", 6379);
    }

    private CacheServiceId getRedisId(String host, int port) {
        return CacheServiceId.redis(host, port);
    }

    public <K, V> void registerCache(CacheServiceId id) {
        cacheRegistry.register(id, new RedisCacheService.Builder<>(null, null, null).build());  // TODO
    }
}
