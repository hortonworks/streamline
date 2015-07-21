package com.hortonworks.iotas.storage;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.catalog.ParserInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class InMemoryStorageManagerTest extends AbstractStoreManagerTest {
    private StorageManager storageManager = new InMemoryStorageManager();

    @Override
    public StorageManager getStorageManager() {
        return storageManager;
    }
}
