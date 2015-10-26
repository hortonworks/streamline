package com.hortonworks.iotas.cache.writer;

import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.StorageManager;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by hlouro on 8/7/15.
 */
public class StorageWriteBehind implements StorageWriter {
    private static final int NUM_THREADS = 5;
    private StorageManager dao;
    private ExecutorService executorService;

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
        private Storable storable;

        AddCallable(Storable storable) {
            this.storable = storable;
        }

        public Storable call() throws Exception {
            dao.add(storable);
            return null;    //TODO since not returning value, perhaps we can use runnable
        }
    }

    private class AddOrUpdateCallable implements Callable<Storable> {
        private Storable storable;

        AddOrUpdateCallable(Storable storable) {
            this.storable = storable;
        }

        public Storable call() throws Exception {
            dao.addOrUpdate(storable);
            return null;    //TODO since not returning value, perhaps we can use runnable
        }
    }

    private class DeleteCallable implements Callable<Storable> {
        private StorableKey key;

        DeleteCallable(StorableKey key) {
            this.key = key;
        }

        public Storable call() throws Exception {
            return dao.remove(key);
        }
    }

}
