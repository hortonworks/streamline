package com.hortonworks.iotas.storage;


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.hortonworks.iotas.service.CatalogService.QueryParam;

//TODO: The synchronization is broken right now, so all the methods dont guarantee the semantics as described in the interface.
public class InMemoryStorageManager implements StorageManager {

    private ConcurrentHashMap<String, ConcurrentHashMap<PrimaryKey, Storable>> storageMap =  new ConcurrentHashMap<String, ConcurrentHashMap<PrimaryKey, Storable>>();
    private ConcurrentHashMap<String, Long> sequenceMap = new ConcurrentHashMap<String, Long>();
    private ConcurrentHashMap<String, Class<?>> nameSpaceClassMap = new ConcurrentHashMap<String, Class<?>>();

    public void add(Storable storable) throws AlreadyExistsException {
        String namespace = storable.getNameSpace();
        PrimaryKey id = storable.getPrimaryKey();
        Storable existing = get(namespace, id);
        if(existing == null) {
            addOrUpdate(storable);
        } else if(existing.equals(storable)) {
            return;
        } else {
            throw new AlreadyExistsException("Another instnace with same id = " + storable.getPrimaryKey() + " exists with different value in namespace " + namespace +
                    " Consider using addOrUpdate method if you always want to overwrite.");
        }
    }

    public <T extends Storable> T remove(String namespace, PrimaryKey id) {
        if(storageMap.containsKey(namespace)) {
            return (T) storageMap.get(namespace).remove(id);
        }
        return null;
    }

    public void addOrUpdate(Storable storable) {
        String namespace = storable.getNameSpace();
        PrimaryKey id = storable.getPrimaryKey();
        if(!storageMap.containsKey(namespace)) {
            storageMap.putIfAbsent(namespace, new ConcurrentHashMap<PrimaryKey, Storable>());
            nameSpaceClassMap.putIfAbsent(namespace, storable.getClass());
        }
        storageMap.get(namespace).put(id, storable);
    }

    public <T extends Storable> T get(String namespace, PrimaryKey id) throws StorageException {
        return storageMap.containsKey(namespace) ? (T) storageMap.get(namespace).get(id) : null;
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
        } catch (InvocationTargetException e) {
            res = false;
        } catch (NoSuchMethodException e) {
            res = false;
        } catch (IllegalAccessException e) {
            res = false;
        }
        return res;
    }

    public <T extends Storable> List<T> find(String namespace, List<QueryParam> queryParams) throws Exception {
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


    public <T extends Storable> Collection<T> list(String namespace) throws StorageException {
        return storageMap.containsKey(namespace) ? (Collection<T>) storageMap.get(namespace).values() : Collections.EMPTY_LIST;
    }

    public void cleanup() throws StorageException {
        //no-op
    }

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
