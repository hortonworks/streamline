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

package com.hortonworks.iotas.cache.view.io.loader;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import com.hortonworks.iotas.cache.Cache;
import com.hortonworks.iotas.cache.view.datastore.DataStoreReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CacheLoaderAsync<K,V> extends CacheLoader<K,V> {
    private static final int DEFAULT_NUM_THREADS = 5;
    private static final Logger LOG = LoggerFactory.getLogger(CacheLoaderAsync.class);

    private ListeningExecutorService executorService;

    public CacheLoaderAsync(Cache<K, V> cache, DataStoreReader<K,V> dataStoreReader) {
        this(cache, dataStoreReader, Executors.newFixedThreadPool(DEFAULT_NUM_THREADS));
    }

    public CacheLoaderAsync(Cache<K, V> cache, DataStoreReader<K,V> dataStoreReader, ExecutorService executorService) {
        super(cache, dataStoreReader);
        this.executorService = MoreExecutors.listeningDecorator(executorService);
    }

    public void loadAll(final Collection<? extends K> keys, CacheLoaderCallback<K,V> callback) {
        try {
            ListenableFuture myCall = executorService.submit(new DataStoreCallable(keys));
            Futures.addCallback(myCall, new CacheLoaderAsyncFutureCallback(keys, callback));

        } catch (Exception e) {
            handleException(keys, callback, e, LOG);
        }
    }

    private class DataStoreCallable implements Callable<Map<K,V>> {
        private Collection<? extends K> keys;

        public DataStoreCallable(Collection<? extends K> keys) {
            this.keys = keys;
        }

        @Override
        public Map<K, V> call() throws Exception {
            final Map<K, V> result = dataStoreReader.readAll(keys);
            LOG.debug("Call to data store for keys [{}] returned [{}]", keys, result);
            return result;
        }
    }

    public class CacheLoaderAsyncFutureCallback implements FutureCallback<Map<K,V>> {
        private final Collection<? extends K> keys;
        private CacheLoaderCallback<K,V> callback;

        public CacheLoaderAsyncFutureCallback(Collection<? extends K> keys, CacheLoaderCallback<K, V> callback) {
            this.keys = keys;
            this.callback = callback;
        }

        @Override
        public void onSuccess(Map<K, V> read) {
            LOG.debug("Raw result of call to data store for keys [{}] returned [{}]", keys, read);
            Map<K,V> loaded = new HashMap<>();

            if (read != null) {
                for (Map.Entry<K, V> re : read.entrySet()) {
                    if (re.getKey() != null) {
                        loaded.put(re.getKey(), re.getValue());
                    } else {
                        LOG.trace("Not loading into cache entry with null value [{}]", re);
                    }
                }
            }

            cache.putAll(loaded);
            LOG.debug("Loaded cache [{}]", loaded);
            callback.onCacheLoaded(loaded);
        }

        @Override
        public void onFailure(Throwable t) {
            callback.onCacheLoadingFailure(t);
        }
    }
}
