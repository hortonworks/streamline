package com.hortonworks.iotas.storage.impl.jdbc.phoenix;

import com.hortonworks.iotas.common.test.HBaseIntegrationTest;
import com.hortonworks.iotas.storage.impl.jdbc.JdbcStorageManagerIntegrationTest;
import com.hortonworks.iotas.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.iotas.storage.impl.jdbc.connection.HikariCPConnectionBuilder;
import com.hortonworks.iotas.storage.impl.jdbc.provider.phoenix.JdbcClient;
import com.hortonworks.iotas.storage.impl.jdbc.provider.phoenix.factory.PhoenixExecutor;
import com.zaxxer.hikari.HikariConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;

/**
 * Phoenix storage integration tests without using cache.
 *
 */
@Category(HBaseIntegrationTest.class)
public abstract class PhoenixStorageManagerNoCacheIntegrationTest extends JdbcStorageManagerIntegrationTest {

    public PhoenixStorageManagerNoCacheIntegrationTest() {
        // setConnectionBuilder();
        jdbcStorageManager = createJdbcStorageManager(new PhoenixExecutor(new ExecutionConfig(-1), connectionBuilder));
    }

    @Before
    public void setUp() throws Exception {
        JdbcClient jdbcClient = new JdbcClient();
        jdbcClient.runScript("phoenix/create_tables.sql");
    }

    @After
    public void tearDown() throws Exception {
        JdbcClient jdbcClient = new JdbcClient();
        jdbcClient.runScript("phoenix/drop_tables.sql");
        jdbcStorageManager.cleanup();
    }


    protected static void setConnectionBuilder() {
        HikariConfig hikariConfig = new HikariConfig();
        try {
            Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        hikariConfig.setJdbcUrl("jdbc:phoenix:localhost:2181");

        connectionBuilder = new HikariCPConnectionBuilder(hikariConfig);
    }


}