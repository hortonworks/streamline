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

package com.hortonworks.streamline.storage.impl.memory;


import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.common.util.ReflectionHelper;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.StorageManager;
import com.hortonworks.streamline.storage.exception.AlreadyExistsException;
import com.hortonworks.streamline.storage.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

//TODO: The synchronization is broken right now, so all the methods don't guarantee the semantics as described in the interface.
public class InMemoryStorageManager implements StorageManager {
    private static final Logger LOG = LoggerFactory.getLogger(InMemoryStorageManager.class);

    private final ConcurrentHashMap<String, ConcurrentHashMap<PrimaryKey, Storable>> storageMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> sequenceMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Class<?>> nameSpaceClassMap = new ConcurrentHashMap<>();

    @Override
    public void init(Map<String, Object> properties) {

    }

    @Override
    public void add(Storable storable) throws AlreadyExistsException {
        final Storable existing = get(storable.getStorableKey());

        if (existing == null) {
            addOrUpdate(storable);
        } else if (!existing.equals(storable)) {
            throw new AlreadyExistsException("Another instance with same id = " + storable.getPrimaryKey()
                    + " exists with different value in namespace " + storable.getNameSpace()
                    + " Consider using addOrUpdate method if you always want to overwrite.");
        }
    }

    @Override
    public <T extends Storable> T remove(StorableKey key) throws StorageException {
        if (storageMap.containsKey(key.getNameSpace())) {
            return (T) storageMap.get(key.getNameSpace()).remove(key.getPrimaryKey());
        }
        return null;
    }

    @Override
    public void addOrUpdate(Storable storable) {
        String namespace = storable.getNameSpace();
        PrimaryKey id = storable.getPrimaryKey();
        if (!storageMap.containsKey(namespace)) {
            storageMap.putIfAbsent(namespace, new ConcurrentHashMap<PrimaryKey, Storable>());
            nameSpaceClassMap.putIfAbsent(namespace, storable.getClass());
        }
        storageMap.get(namespace).put(id, storable);
    }

    @Override
    public <T extends Storable> T get(StorableKey key) throws StorageException {
        return storageMap.containsKey(key.getNameSpace())
                ? (T) storageMap.get(key.getNameSpace()).get(key.getPrimaryKey())
                : null;
    }

    /**
     * Uses reflection to query the field or the method. Assumes
     * a public getXXX method is available to get the field value.
     */
    private boolean matches(Storable val, List<QueryParam> queryParams, Class<?> clazz) {
        Object fieldValue;
        boolean res = true;
            for (QueryParam qp : queryParams) {
                try {
                    fieldValue = ReflectionHelper.invokeGetter(qp.name, val);
                    if (!fieldValue.toString().equals(qp.value)) {
                        return false;
                    }
                } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                    LOG.error("FAILED to invoke getter for query param {} , is your param name correct?", qp.getName(), e);
                    return false;
                }
            }
        return res;
    }

    public <T extends Storable> Collection<T> find(String namespace, List<QueryParam> queryParams) throws StorageException {
        Collection<T> result = new ArrayList<>();
        if (queryParams == null) {
            result = list(namespace);
        } else {
            Class<?> clazz = nameSpaceClassMap.get(namespace);
            if (clazz != null) {
                Map<PrimaryKey, Storable> storableMap = storageMap.get(namespace);
                if (storableMap != null) {
                    for (Storable val : storableMap.values()) {
                        if (matches(val, queryParams, clazz)) {
                            result.add((T) val);
                        }
                    }
                }
            }
        }
        return result;
    }


    @Override
    public <T extends Storable> Collection<T> list(String namespace) throws StorageException {
        return storageMap.containsKey(namespace)
                ? (Collection<T>) storageMap.get(namespace).values() : Collections.<T>emptyList();
    }

    @Override
    public void cleanup() throws StorageException {
        //no-op
    }

    /**
     * atomically increment and return the next id for the given namespace
     */
    @Override
    public Long nextId(String namespace) {
        AtomicLong cur = sequenceMap.get(namespace);
        if (cur == null) {
            AtomicLong zero = new AtomicLong();
            cur = sequenceMap.putIfAbsent(namespace, zero);
            if (cur == null) {
                cur = zero;
            }
        }
        return cur.incrementAndGet();
    }

    @Override
    public void registerStorables(Collection<Class<? extends Storable>> classes) throws StorageException {
    }

}
