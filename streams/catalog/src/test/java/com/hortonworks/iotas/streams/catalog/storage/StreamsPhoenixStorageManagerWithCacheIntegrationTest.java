package com.hortonworks.iotas.streams.catalog.storage;

import com.hortonworks.iotas.common.test.HBaseIntegrationTest;
import com.hortonworks.iotas.storage.impl.jdbc.JdbcStorageManager;
import com.hortonworks.iotas.storage.impl.jdbc.phoenix.PhoenixStorageManagerWithCacheIntegrationTest;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.factory.QueryExecutor;
import com.hortonworks.iotas.streams.catalog.service.StreamCatalogService;
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