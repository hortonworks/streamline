package com.hortonworks.streamline.streams.catalog.storage;

import com.hortonworks.streamline.common.test.HBaseIntegrationTest;
import com.hortonworks.streamline.storage.impl.jdbc.JdbcStorageManager;
import com.hortonworks.streamline.storage.impl.jdbc.phoenix.PhoenixStorageManagerWithCacheIntegrationTest;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.factory.QueryExecutor;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
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