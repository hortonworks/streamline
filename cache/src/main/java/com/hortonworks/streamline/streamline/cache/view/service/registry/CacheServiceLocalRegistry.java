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

package com.hortonworks.streamline.cache.view.service.registry;

import com.hortonworks.streamline.cache.view.service.CacheService;
import com.hortonworks.streamline.cache.view.service.CacheServiceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/* Local {@link CacheServiceRegistry} thread safe singleton */
public enum CacheServiceLocalRegistry implements CacheServiceRegistry {
    INSTANCE;

    private static final Logger LOG = LoggerFactory.getLogger(CacheServiceLocalRegistry.class);

    private final ConcurrentMap<CacheServiceId, CacheService<?,?>> serviceIdToService;

    CacheServiceLocalRegistry() {
        serviceIdToService = new ConcurrentHashMap<>();
    }

    // TODO: Handle attempting to put an object with an already existing id
    public <K,V> void register(CacheServiceId cacheServiceId, CacheService<K,V> cacheService) {
        serviceIdToService.putIfAbsent(cacheServiceId, cacheService);
        LOG.info("Registered cache service [{}] with id [{}].", cacheService, cacheServiceId);
    }

    public <K,V> CacheService<K,V> getCacheService(CacheServiceId cacheServiceId) {
        return (CacheService<K, V>) serviceIdToService.get(cacheServiceId);
    }
}
