package com.hortonworks.streamline.streams.catalog.storage;

import com.hortonworks.streamline.common.test.HBaseIntegrationTest;
import com.hortonworks.streamline.storage.StorableTest;
import com.hortonworks.streamline.storage.exception.NonIncrementalColumnException;
import com.hortonworks.streamline.storage.impl.jdbc.JdbcStorageManager;
import com.hortonworks.streamline.storage.impl.jdbc.phoenix.PhoenixStorageManagerWithCacheIntegrationTest;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.factory.QueryExecutor;
import com.hortonworks.streamline.streams.catalog.service.CatalogService;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 *
 */

@Category(HBaseIntegrationTest.class)
public class CorePhoenixStorageManagerWithCacheIntegrationTest extends PhoenixStorageManagerWithCacheIntegrationTest {

    @Override
    protected void setStorableTests() {
        storableTests = new CatalogTests().getAllTests();
    }

    public  JdbcStorageManager createJdbcStorageManager(QueryExecutor queryExecutor) {
        JdbcStorageManager jdbcStorageManager = new JdbcStorageManager(queryExecutor);
        jdbcStorageManager.registerStorables(CatalogService.getStorableClasses());
        return jdbcStorageManager;
    }

    @Test(expected = NonIncrementalColumnException.class)
    public void testNextId_NoAutoincrementTable_NonIncrementalKeyException() throws Exception {
        for (StorableTest test : storableTests) {
            if (test instanceof CatalogTests.FilesTest) {
                getStorageManager().nextId(test.getNameSpace());    // should throw exception
            }
        }
    }
}