package org.apache.streamline.storage.cache.writer;

import org.apache.streamline.storage.Storable;
import org.apache.streamline.storage.StorableKey;

/**
 * Created by hlouro on 8/7/15.
 */
public interface StorageWriter {
    void add(Storable storable);

    void addOrUpdate(Storable storable);

    Object remove(StorableKey key);
}
