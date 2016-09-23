package com.hortonworks.iotas.storage.cache.writer;

import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;

/**
 * Created by hlouro on 8/7/15.
 */
public interface StorageWriter {
    void add(Storable storable);

    void addOrUpdate(Storable storable);

    Object remove(StorableKey key);
}
