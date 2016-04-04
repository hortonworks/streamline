package com.hortonworks.iotas.storage.impl.jdbc.phoenix;

import com.hortonworks.iotas.catalog.DataFeed;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.catalog.Topology;
import com.hortonworks.iotas.storage.impl.jdbc.JdbcStorageManager;
import com.hortonworks.iotas.storage.impl.jdbc.JdbcStorageManagerIntegrationTest;
import com.hortonworks.iotas.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.iotas.storage.impl.jdbc.connection.HikariCPConnectionBuilder;
import com.hortonworks.iotas.storage.impl.jdbc.provider.phoenix.JdbcClient;
import com.hortonworks.iotas.storage.impl.jdbc.provider.phoenix.factory.PhoenixExecutor;
import com.hortonworks.iotas.test.HBaseIntegrationTest;
import com.hortonworks.iotas.topology.TopologyComponent;
import com.zaxxer.hikari.HikariConfig;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Phoenix storage integration tests without using cache.
 *
 */
@Category(HBaseIntegrationTest.class)
public class PhoenixStorageManagerNoCacheIntegrationTest extends JdbcStorageManagerIntegrationTest {

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

    @BeforeClass
    public static void setUpClass() throws Exception {
        setConnectionBuilder();
        jdbcStorageManager = new JdbcStorageManager(new PhoenixExecutor(new ExecutionConfig(-1), connectionBuilder));
    }

    protected static void setConnectionBuilder() throws ClassNotFoundException {
        HikariConfig hikariConfig = new HikariConfig();
        Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
        hikariConfig.setJdbcUrl("jdbc:phoenix:localhost:2181");

        connectionBuilder = new HikariCPConnectionBuilder(hikariConfig);
    }

    @Test
    public void testNextId_AutoincrementColumn_IdPlusOne() throws Exception {
        final PhoenixExecutor phoenixExecutor = new PhoenixExecutor(new ExecutionConfig(-1), connectionBuilder);
        String[] nameSpaces = {DataSource.NAME_SPACE, DataFeed.NAME_SPACE, ParserInfo.NAME_SPACE, Topology.NAME_SPACE, TopologyComponent.NAME_SPACE};
        for (String nameSpace : nameSpaces) {
            log.info("Generating sequence-ids for namespace: [{}]", nameSpace);
            for (int x = 0; x < 100; x++) {
                final Long nextId = phoenixExecutor.nextId(nameSpace);
                log.info("\t\t[{}]", nextId);
                Assert.assertTrue(nextId > 0);
            }
        }
    }

}