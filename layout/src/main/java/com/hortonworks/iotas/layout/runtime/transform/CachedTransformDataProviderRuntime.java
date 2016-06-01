/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.layout.runtime.transform;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This class creates a loadable cache for given backing {@link TransformDataProviderRuntime} with caching configuration like maximum size, expiration interval
 * and refresh interval.
 */
public class CachedTransformDataProviderRuntime implements TransformDataProviderRuntime {

    private final TransformDataProviderRuntime backedTransformDataProviderRuntime;
    private final long maxCacheSize;
    private final long entryExpirationInterval;
    private final long refreshInterval;

    private LoadingCache<Object, Object> loadingCache;

    /**
     * Creates CachedDataProvider.
     *
     * @param backedTransformDataProviderRuntime DataProvider to be facaded with caching
     * @param maxCacheSize maximum cache size
     * @param entryExpirationInterval expiration interval in seconds for each entry
     * @param entryRefreshInterval refresh interval in seconds for an entry
     */
    public CachedTransformDataProviderRuntime(TransformDataProviderRuntime backedTransformDataProviderRuntime, long maxCacheSize, long entryExpirationInterval, long entryRefreshInterval) {
        this.backedTransformDataProviderRuntime = backedTransformDataProviderRuntime;
        this.maxCacheSize = maxCacheSize;
        this.entryExpirationInterval = entryExpirationInterval;
        this.refreshInterval = entryRefreshInterval;
    }

    @Override
    public void prepare() {
        backedTransformDataProviderRuntime.prepare();
        loadingCache =
                CacheBuilder.newBuilder()
                        .maximumSize(maxCacheSize)
                        .refreshAfterWrite(refreshInterval, TimeUnit.SECONDS)
                        .expireAfterWrite(entryExpirationInterval, TimeUnit.SECONDS)
                        .build(new CacheLoader<Object, Object>() {
                            @Override
                            public Object load(Object key) throws Exception {
                                return backedTransformDataProviderRuntime.get(key);
                            }
                        });

    }

    @Override
    public Object get(Object key) {
        try {
            return loadingCache.get(key);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cleanup() {
        loadingCache.cleanUp();
        backedTransformDataProviderRuntime.cleanup();
    }

}
