package org.apache.streamline.streams.catalog.storage;

import org.apache.streamline.common.test.HBaseIntegrationTest;
import org.apache.registries.storage.impl.jdbc.JdbcStorageManager;
import org.apache.registries.storage.impl.jdbc.phoenix.PhoenixStorageManagerWithCacheIntegrationTest;
import org.apache.registries.storage.impl.jdbc.provider.sql.factory.QueryExecutor;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.junit.experimental.categories.Category;

/**
 *
 */

@Category(HBaseIntegrationTest.class)
public  class StreamsPhoenixStorageManagerWithCacheIntegrationTest extends PhoenixStorageManagerWithCacheIntegrationTest {

    @Override
    protected void setStorableTests() {
        storableTests = new StreamCatagoryTests().getAllTests();
    }

    public JdbcStorageManager createJdbcStorageManager(QueryExecutor queryExecutor) {
        JdbcStorageManager jdbcStorageManager = new JdbcStorageManager(queryExecutor);
        jdbcStorageManager.registerStorables(StreamCatalogService.getStorableClasses());
        return jdbcStorageManager;
    }
}