package com.hortonworks.iotas.storage;


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.hortonworks.iotas.service.CatalogService.QueryParam;

//TODO: The synchronization is broken right now, so all the methods dont guarantee the semantics as described in the interface.
public class InMemoryStorageManager implements StorageManager {

    private ConcurrentHashMap<String, ConcurrentHashMap<PrimaryKey, Storable>> storageMap =  new ConcurrentHashMap<String, ConcurrentHashMap<PrimaryKey, Storable>>();
    private ConcurrentHashMap<String, Long> sequenceMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Class<?>> nameSpaceClassMap = new ConcurrentHashMap<String, Class<?>>();

    @Override
    public void add(Storable storable) throws AlreadyExistsException {
        final Storable existing = get(storable.getStorableKey());

        if(existing == null) {
            addOrUpdate(storable);
        } else if (existing.equals(storable)) {
            return;
        } else {
            throw new AlreadyExistsException("Another instnace with same id = " + storable.getPrimaryKey()
                    + " exists with different value in namespace " + storable.getNameSpace()
                    + " Consider using addOrUpdate method if you always want to overwrite.");
        }
    }

    @Override
    public <T extends Storable> T  remove(StorableKey key) throws StorageException {
        if(storageMap.containsKey(key.getNameSpace())) {
            return (T) storageMap.get(key.getNameSpace()).remove(key.getPrimaryKey());
        }
        return null;
    }

    @Override
    public void addOrUpdate(Storable storable) {
        String namespace = storable.getNameSpace();
        PrimaryKey id = storable.getPrimaryKey();
        if(!storageMap.containsKey(namespace)) {
            storageMap.putIfAbsent(namespace, new ConcurrentHashMap<PrimaryKey, Storable>());
            nameSpaceClassMap.putIfAbsent(namespace, storable.getClass());
        }
        storageMap.get(namespace).put(id, storable);
    }

    @Override
    public <T extends Storable> T  get(StorableKey key) throws StorageException {
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
        try {
            for (QueryParam qp : queryParams) {
                String methodName = new StringBuilder("get")
                        .append(qp.name.substring(0, 1).toUpperCase())
                        .append(qp.name.substring(1)).toString();
                fieldValue = clazz.getDeclaredMethod(methodName).invoke(val);
                if (!fieldValue.toString().equals(qp.value)) {
                    return false;
                }
            }
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            res = false;
        }
        return res;
    }

    public <T extends Storable> Collection<T> find(String namespace, List<QueryParam> queryParams) throws StorageException {
        List<Storable> result = new ArrayList<Storable>();
        Class<?> clazz = nameSpaceClassMap.get(namespace);
        if(clazz != null) {
            Map<PrimaryKey, Storable> storableMap = storageMap.get(namespace);
            if (storableMap != null) {
                for (Storable val : storableMap.values()) {
                    if (matches(val, queryParams, clazz)) {
                        result.add(val);
                    }
                }
            }
        }
        return  (List<T>) result;
    }


    @Override
    public <T extends Storable> Collection<T> list(String namespace) throws StorageException {
        return (Collection<T>) (storageMap.containsKey(namespace)
                        ? storageMap.get(namespace).values()
                        : new ArrayList<Storable>());
    }

    @Override
    public void cleanup() throws StorageException {
        //no-op
    }

    @Override
    public Long nextId(String namespace){
        Long id = this.sequenceMap.get(namespace);
        if(id == null){
            id = 0l;
        }
        id++;
        this.sequenceMap.put(namespace, id);
        return id;
    }
}
