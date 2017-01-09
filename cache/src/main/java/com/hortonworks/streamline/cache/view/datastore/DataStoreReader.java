package com.hortonworks.streamline.cache.view.datastore;

import java.util.Collection;
import java.util.Map;

public interface DataStoreReader<K, V> {
    V read(K key);

    Map<K, V> readAll(Collection<? extends K> keys);
}
