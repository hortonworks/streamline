package com.hortonworks.iotas.storage.impl.jdbc.phoenix;

import com.google.common.cache.CacheBuilder;
import com.hortonworks.iotas.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.iotas.storage.impl.jdbc.provider.phoenix.factory.PhoenixExecutor;
import com.hortonworks.iotas.test.HBaseIntegrationTest;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;

/**
 *
 */

@Category(HBaseIntegrationTest.class)
public class PhoenixStorageManagerWithCacheIntegrationTest extends PhoenixStorageManagerNoCacheIntegrationTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        setConnectionBuilder();
        CacheBuilder  cacheBuilder = CacheBuilder.newBuilder().maximumSize(3);
        jdbcStorageManager = createJdbcStorageManager(new PhoenixExecutor(new ExecutionConfig(-1), connectionBuilder, cacheBuilder));
    }

}