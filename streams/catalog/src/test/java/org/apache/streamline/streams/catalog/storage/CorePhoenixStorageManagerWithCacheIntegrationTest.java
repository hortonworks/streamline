package org.apache.streamline.streams.catalog.storage;

import org.apache.streamline.common.test.HBaseIntegrationTest;
import org.apache.streamline.storage.StorableTest;
import org.apache.streamline.storage.exception.NonIncrementalColumnException;
import org.apache.streamline.storage.impl.jdbc.JdbcStorageManager;
import org.apache.streamline.storage.impl.jdbc.phoenix.PhoenixStorageManagerWithCacheIntegrationTest;
import org.apache.streamline.storage.impl.jdbc.provider.sql.factory.QueryExecutor;
import org.apache.streamline.streams.catalog.service.CatalogService;
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
            if (test instanceof CatalogTests.DeviceTest) {
                getStorageManager().nextId(test.getNameSpace());    // should throw exception
            }
        }
    }

    @Test
    public void testNextId_AutoincrementColumn_IdPlusOne() throws Exception {

        for (StorableTest test : storableTests) {
            // Device does not have auto_increment, and therefore there is no concept of nextId and should throw exception (tested below)
            if (!(test instanceof CatalogTests.DeviceTest)) {
                doTestNextId_AutoincrementColumn_IdPlusOne(test);
            }
        }
    }


}