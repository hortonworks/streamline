/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
**/
package com.hortonworks.streamline.storage.cache.writer;

import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.StorageManager;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by hlouro on 8/7/15.
 */
public class StorageWriteBehind implements StorageWriter {
    private static final int NUM_THREADS = 5;
    private final StorageManager dao;
    private final ExecutorService executorService;

    public StorageWriteBehind(StorageManager dao) {
        this(dao, Executors.newFixedThreadPool(NUM_THREADS));
    }

    public StorageWriteBehind(StorageManager dao, ExecutorService executorService) {
        this.dao = dao;
        this.executorService = executorService;
    }

    public void add(Storable storable) {
        executorService.submit(new AddCallable(storable));
    }

    public void addOrUpdate(Storable storable) {
        executorService.submit(new AddOrUpdateCallable(storable));
    }

    public Object remove(StorableKey key) {
        return executorService.submit(new DeleteCallable(key));
    }

    // ======= Callables for async writing to the Storage Layer =============

    private class AddCallable implements Callable<Storable> {
        private final Storable storable;

        AddCallable(Storable storable) {
            this.storable = storable;
        }

        public Storable call() throws Exception {
            dao.add(storable);
            return null;    //TODO since not returning value, perhaps we can use runnable
        }
    }

    private class AddOrUpdateCallable implements Callable<Storable> {
        private final Storable storable;

        AddOrUpdateCallable(Storable storable) {
            this.storable = storable;
        }

        public Storable call() throws Exception {
            dao.addOrUpdate(storable);
            return null;    //TODO since not returning value, perhaps we can use runnable
        }
    }

    private class DeleteCallable implements Callable<Storable> {
        private final StorableKey key;

        DeleteCallable(StorableKey key) {
            this.key = key;
        }

        public Storable call() throws Exception {
            return dao.remove(key);
        }
    }

}
