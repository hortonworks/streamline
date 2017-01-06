package com.hortonworks.streamline.storage.cache.writer;

import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.StorageManager;

/**
 * Created by hlouro on 8/7/15.
 */
public class StorageWriteThrough implements StorageWriter {
    private final StorageManager dao;

    public StorageWriteThrough(StorageManager dao) {
        this.dao = dao;
    }

    public void add(Storable storable) {
        dao.add(storable);
    }

    public void addOrUpdate(Storable storable) {
        dao.addOrUpdate(storable);
    }

    public Object remove(StorableKey key) {
        return dao.remove(key);
    }
}
