package com.hortonworks.iotas.storage;

import com.hortonworks.iotas.common.QueryParam;
import com.hortonworks.iotas.common.Schema;
import org.junit.Assert;

import java.util.*;

public class StorableTest {
    protected List<Storable> storableList;
    protected  StorageManager storageManager;

    /**
     * Performs any initialization steps that are required to test this storable instance, for example,
     * initialize the a parent table that a child table refers to (e.g. DataSource and Device)
     */
    public void init() { }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public void setStorageManager(StorageManager storeManager) {
        this.storageManager = storeManager;
    }

    /**
     * Each of the storable entities has its own list and the 0th and 1st index items in that list has same id so
     * test will use that to test the update operation. the 3rd item is inserted at storage layer and 4th i
     */
    public void test() {
        final Storable storable1 = storableList.get(0);
        final Storable storable2 = storableList.get(1);
        final Storable storable3 = storableList.get(2);
        final Storable storable4 = storableList.get(3);
        String namespace = storable1.getNameSpace();

        // test get nonexistent key
        Assert.assertNull(getStorageManager().get(storable1.getStorableKey()));

        //test add, get by inserting the first item in list.
        getStorageManager().add(storable1);
        Assert.assertEquals(storable1, getStorageManager().get(storable1.getStorableKey()));

        //test update by calling addOrUpdate on second item which should have the same primary key value as the first item.
        Assert.assertEquals(storable1.getPrimaryKey(), storable2.getPrimaryKey());
        getStorageManager().addOrUpdate(storable2);
        Assert.assertEquals(storable2, getStorageManager().get(storable2.getStorableKey()));

        //add 3rd item, only added so that list operation will return more then one item.
        getStorageManager().addOrUpdate(storable3);
        Assert.assertEquals(storable3, getStorageManager().get(storable3.getStorableKey()));

        //test remove by adding 4th item and removing it.
        getStorageManager().addOrUpdate(storable4);
        Assert.assertEquals(storable4, getStorageManager().get(storable4.getStorableKey()));
        Storable removed = getStorageManager().remove(storable4.getStorableKey());
        Assert.assertNull(getStorageManager().get(storable4.getStorableKey()));
        // check that the correct removed item gets returned
        Assert.assertEquals(storable4, removed);

        //Test list method. The storage layer should have the 2nd item (updated version of 1st item) and 3rd Item.
        final Set<Storable> expected = new HashSet<Storable>() {{
            add(storable2);
            add(storable3);
        }};
        final HashSet allExisting = new HashSet(getStorageManager().list(getNameSpace()));
        Assert.assertEquals(expected, allExisting);

        //Test method with query parameters(filter) matching only the item storable3
        final Collection<Storable> found = getStorageManager().find(namespace, buildQueryParamsForPrimaryKey(storable3));
        Assert.assertEquals(1, found.size());
        Assert.assertTrue(found.contains(storable3));
    }

    public void close() {
        getStorageManager().cleanup();
    }

    public List<Storable> getStorableList() {
        return storableList;
    }

    public void addAllToStorage() {
        for (Storable storable : storableList) {
            getStorageManager().addOrUpdate(storable);
        }
    }

    protected List<QueryParam> buildQueryParamsForPrimaryKey(Storable storable) {
        final Map<Schema.Field, Object> fieldsToVal = storable.getPrimaryKey().getFieldsToVal();
        final List<QueryParam> queryParams = new ArrayList<>(fieldsToVal.size());

        for (Schema.Field field : fieldsToVal.keySet()) {
            QueryParam qp = new QueryParam(field.getName(), fieldsToVal.get(field).toString());
            queryParams.add(qp);
        }

        return queryParams;
    }

    public String getNameSpace() {
        return storableList.get(0).getStorableKey().getNameSpace();
    }
}

