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

package com.hortonworks.iotas.cache.view.io.writer;

import com.hortonworks.iotas.cache.view.datastore.DataStoreWriter;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CacheWriterAsync<K, V> implements CacheWriter<K, V> {
    private static final int DEFAULT_NUM_THREADS = 5;

    private final DataStoreWriter<K, V> dataStoreWriter;
    private final ExecutorService executorService;

    public CacheWriterAsync(DataStoreWriter<K, V> dataStoreWriter) {
        this(dataStoreWriter, Executors.newFixedThreadPool(DEFAULT_NUM_THREADS));
    }

    public CacheWriterAsync(DataStoreWriter<K, V> dataStoreWriter, ExecutorService executorService) {
        this.dataStoreWriter = dataStoreWriter;
        this.executorService = executorService;
    }

    public void write(final K key, final V val) {
        executorService.submit(new DataStoreWriteRunnable(key, val));
    }

    public void writeAll(Map<? extends K, ? extends V> entries) {
        executorService.submit(new DataStoreWriteRunnable(entries));
    }

    public void delete(final K key) {
        executorService.submit(new DataStoreDeleteRunnable(key));
    }

    public void deleteAll(Collection<? extends K> keys) {
        executorService.submit(new DataStoreDeleteRunnable(keys));
    }

    private class DataStoreWriteRunnable implements Runnable {
        private Map<? extends K, ? extends V> entries;
        private K key;
        private V val;

        public DataStoreWriteRunnable(K key, V val) {
            this.key = key;
            this.val = val;
        }

        public DataStoreWriteRunnable(Map<? extends K, ? extends V> entries) {
            this.entries = entries;
        }

        @Override
        public void run() {
            if (entries != null) {
                dataStoreWriter.writeAll(entries);
            } else {
                dataStoreWriter.write(key, val);
            }
        }
    }

    private class DataStoreDeleteRunnable implements Runnable {
        private Collection<? extends K> keys;
        private K key;

        public DataStoreDeleteRunnable(K key) {
            this.key = key;
        }

        public DataStoreDeleteRunnable(Collection<? extends K> keys) {
            this.keys = keys;
        }

        @Override
        public void run() {
            if (keys != null) {
                dataStoreWriter.deleteAll(keys);
            } else {
                dataStoreWriter.delete(key);
            }
        }
    }
}

