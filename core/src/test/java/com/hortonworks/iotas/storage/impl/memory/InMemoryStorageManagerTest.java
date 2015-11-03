package com.hortonworks.iotas.storage.impl.memory;

import com.hortonworks.iotas.storage.AbstractStoreManagerTest;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorageManager;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

public class InMemoryStorageManagerTest extends AbstractStoreManagerTest {
    private StorageManager storageManager = new InMemoryStorageManager();

    @Override
    public StorageManager getStorageManager() {
        return storageManager;
    }

    @Test
    public void testList_NonexistentNameSpace_StorageException() {
        Collection<Storable> found = getStorageManager().list("NONEXISTENT_NAME_SPACE");
        Assert.assertTrue(found.isEmpty());
    }

    @Override
    protected void setStorableTests() {
        storableTests = new ArrayList<StorableTest>() {{
            add(new DataSourceTest());
            add(new DeviceTest());
            add(new ParsersTest());
            add(new DataFeedsTest());
        }};
    }
}
