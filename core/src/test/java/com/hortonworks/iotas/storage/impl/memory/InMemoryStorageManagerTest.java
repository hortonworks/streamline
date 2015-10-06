package com.hortonworks.iotas.storage.impl.memory;

import com.hortonworks.iotas.storage.AbstractStoreManagerTest;
import com.hortonworks.iotas.storage.StorageManager;

import java.util.ArrayList;

public class InMemoryStorageManagerTest extends AbstractStoreManagerTest {
    private StorageManager storageManager = new InMemoryStorageManager();

    @Override
    public StorageManager getStorageManager() {
        return storageManager;
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
