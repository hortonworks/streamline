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


package com.hortonworks.streamline.cache.view.datastore;

import java.util.Collection;
import java.util.Map;

public abstract class AbstractDataStore<K,V> implements DataStoreReader<K,V>, DataStoreWriter<K,V> {
    private final String nameSpace;

    public AbstractDataStore(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    public String getNameSpace() {
        return nameSpace;
    }

    public abstract V read(K key);

    public abstract Map<K, V> readAll(Collection<? extends K> keys);

    public abstract void write(K key, V val);

    public abstract void writeAll(Map<? extends K, ? extends V> entries);

    public abstract void delete(K key);

    public abstract void deleteAll(Collection<? extends K> keys);
}
