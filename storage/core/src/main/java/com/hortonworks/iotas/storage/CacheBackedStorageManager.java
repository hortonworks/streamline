package com.hortonworks.iotas.storage;

import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.cache.Cache;
import com.hortonworks.iotas.storage.cache.impl.GuavaCache;
import com.hortonworks.iotas.storage.cache.writer.StorageWriter;
import com.hortonworks.iotas.storage.exception.StorageException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by hlouro on 8/7/15.
 */

public class CacheBackedStorageManager implements StorageManager {
    private final StorageWriter writer;
    private final Cache<StorableKey, Storable> cache;
    private final StorageManager dao;

    public CacheBackedStorageManager(Cache<StorableKey, Storable> cache, StorageWriter storageWriter) {
        if (cache == null || storageWriter == null) {
            throw new IllegalArgumentException("Cache and storage writer objects must not be null");
        }
        this.cache = cache;
        this.dao = ((GuavaCache)cache).getDao();
        this.writer = storageWriter;
    }

    @Override
    public void init(Map<String, Object> properties) {

    }

    //TODO: Exception handling in add, remove, addOrUpdate, ...
    @Override
    public void add(Storable storable) throws StorageException {
        writer.add(storable);
        cache.put(storable.getStorableKey(), storable);
    }

    @Override
    public <T extends Storable> T remove(StorableKey key) throws StorageException {
        writer.remove(key);
        final T oldVal = (T) cache.get(key);
        cache.remove(key);
        return oldVal;
    }

    @Override
    public void addOrUpdate(Storable storable) throws StorageException {;
        writer.addOrUpdate(storable);
        cache.put(storable.getStorableKey(), storable);
    }

    @Override
    public <T extends Storable> T get(StorableKey key) throws StorageException {
        return (T) cache.get(key);
    }

    @Override   //TODO:
    public <T extends Storable> Collection<T> find(String namespace, List<QueryParam> queryParams) throws StorageException {
        //Adding workaround methods that calls dao until we figure out what needs to be in caching so the topologies can work.
        return ((GuavaCache)cache).getDao().find(namespace, queryParams);

    }

    @Override
    public <T extends Storable> Collection<T> list(String namespace) throws StorageException {
        return dao.list(namespace);
    }

    @Override
    public void cleanup() throws StorageException {
//        writer.removeAll();       // TODO:
        cache.clear();
    }

    @Override
    public Long nextId(String namespace) throws StorageException {
        return dao.nextId(namespace);
    }

    @Override
    public void registerStorables(Collection<Class<? extends Storable>> classes) throws StorageException {
        dao.registerStorables(classes);
    }
}
