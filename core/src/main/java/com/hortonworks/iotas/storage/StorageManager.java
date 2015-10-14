package com.hortonworks.iotas.storage;

import com.hortonworks.iotas.storage.exception.StorageException;

import java.util.Collection;
import java.util.List;

import static com.hortonworks.iotas.service.CatalogService.QueryParam;

/**
 * TODO: All the methods are very restrictive and needs heavy synchronization to get right but my assumption is that
 * Writes are infrequent and the best place to achieve these guarantees, if we ever need it, is going to be at storage
 * layer implementation it self.
 *
 * I am still not done thinking through the semantics so don't assume this to be concrete APIs.
 */
public interface StorageManager {
    /**
     * TODO: update this javadoc
     * Adds this storable to storage layer, if the storable with same {@code Storable.getPrimaryKey} exists,
     * it will do a get and ensure new and old storables instances are equal in which case it will return false.
     * If the old and new instances are not the same it will throw {@code AlreadyExistsException}.
     * Will return true if no existing storable entity was found and the supplied entity gets added successfully.
     * @param storable
     * @return
     * @throws StorageException
     */
    void add(Storable storable) throws StorageException;

    /**
     * Removes a {@link Storable} object identified by a {@link StorableKey}.
     * If the key does not exist a null value is returned, no exception is thrown.
     *
     * @param key of the {@link Storable} object to remove
     * @return object that got removed, null if no object was removed.
     * @throws StorageException
     */
    <T extends Storable> T remove(StorableKey key) throws StorageException;

    /**
     * Unlike add, if the storage entity already exists, it will be updated. If it does not exist, it will be created.
     * @param storable
     * @return
     * @throws StorageException
     */
    void addOrUpdate(Storable storable) throws StorageException;

    //TODO I need a 101 on java generics right no to ensure this is how I want to deal with these things.
    /**
     * Gets the storable entity by using {@code Storable.getPrimaryKey()} as lookup key, return null if no storable entity with
     * the supplied key is found.
     * @param key
     * @return
     * @throws StorageException
     */
    <T extends Storable> T get(StorableKey key) throws StorageException;

    /**
     * Get the list of storable entities in the namespace, matching the query params.
     * <pre>
     * E.g get a list of all devices with deviceId="nest" and version=1
     *
     * List<QueryParam> params = Arrays.asList(new QueryParam("deviceId", "nest"), new QueryParam("version", "1");
     *
     * List<Device> devices = find(DEVICE_NAMESPACE, params);
     * </pre>
     *
     * @param namespace
     * @param queryParams
     * @return
     * @throws Exception
     */
    <T extends Storable> Collection<T> find(String namespace, List<QueryParam> queryParams) throws StorageException;

    /**
     * Lists all {@link Storable} objects existing in the given namespace. If no entity is found, and empty list will be returned.
     * @param namespace
     * @return
     * @throws StorageException
     */
    <T extends Storable> Collection<T> list(String namespace) throws StorageException;

    //TODO: What is the purpose of this clenup method? Whipe out the DB?
    void cleanup() throws StorageException;

    Long nextId(String namespace) throws StorageException;
}
