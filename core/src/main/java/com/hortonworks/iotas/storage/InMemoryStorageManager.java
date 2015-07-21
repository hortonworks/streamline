package com.hortonworks.iotas.storage;


import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

//TODO: The synchronization is broken right now, so all the methods dont guarantee the semantics as described in the interface.
//I need to go back and read generics to get the Types correct for now just using the superType storage.
public class InMemoryStorageManager implements StorageManager {

    private ConcurrentHashMap<String, ConcurrentHashMap<PrimaryKey, Storable>> storageMap =  new ConcurrentHashMap<String, ConcurrentHashMap<PrimaryKey, Storable>>();

    public void add(Storable storable) throws AlreadyExistsException {
        String namespace = storable.getNameSpace();
        PrimaryKey id = storable.getPrimaryKey();
        Storable existing = get(namespace, id, storable.getClass());
        if(existing == null) {
            addOrUpdate(storable);
        } else if(existing.equals(storable)) {
            return;
        } else {
            throw new AlreadyExistsException("Another instnace with same id = " + storable.getPrimaryKey() + " exists with different value in namespace " + namespace +
                    " Consider using addOrUpdate method if you always want to overwrite.");
        }
    }

    public void remove(String namespace, PrimaryKey id) {
        if(storageMap.containsKey(namespace)) {
            storageMap.get(namespace).remove(id);
        }
    }

    public void addOrUpdate(Storable storable) {
        String namespace = storable.getNameSpace();
        PrimaryKey id = storable.getPrimaryKey();
        if(!storageMap.containsKey(namespace)) {
            storageMap.putIfAbsent(namespace, new ConcurrentHashMap<PrimaryKey, Storable>());
        }
        storageMap.get(namespace).put(id, storable);
    }

    public <T extends Storable> T get(String namespace, PrimaryKey id, Class<T> clazz) throws StorageException {
        return storageMap.containsKey(namespace) ? (T) storageMap.get(namespace).get(id) : null;
    }

    public <T extends Storable> Collection<T> list(String namespace, Class<T> clazz) throws StorageException {
        return (Collection<T>) storageMap.get(namespace).values();
    }

    public void cleanup() throws StorageException {
        //no-op
    }

}
