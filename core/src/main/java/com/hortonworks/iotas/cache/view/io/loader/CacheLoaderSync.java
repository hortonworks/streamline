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

import com.hortonworks.iotas.cache.Cache;
import com.hortonworks.iotas.cache.view.datastore.DataStoreReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

public class CacheLoaderSync<K,V> extends CacheLoader<K,V> {
    protected static final Logger LOG = LoggerFactory.getLogger(CacheLoaderSync.class);

    public CacheLoaderSync(Cache<K, V> cache, DataStoreReader<K,V> dataStoreReader) {
        super(cache, dataStoreReader);
    }

    public void loadAll(Collection<? extends K> keys, CacheLoaderCallback<K,V> callback) {
        Map<K, V> entries;
        try {
            entries = dataStoreReader.readAll(keys);
            cache.putAll(entries);
            callback.onCacheLoaded(entries);
        } catch (Exception e) {
            handleException(keys, callback, e, LOG);
        }
    }
}
