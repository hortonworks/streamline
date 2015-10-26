/*
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hortonworks.iotas.storage.impl.jdbc;

import com.google.common.cache.CacheBuilder;
import com.hortonworks.iotas.storage.AbstractStoreManagerTest;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorageManager;
import com.hortonworks.iotas.storage.exception.NonIncrementalColumnException;
import com.hortonworks.iotas.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.iotas.storage.impl.jdbc.connection.ConnectionBuilder;
import com.hortonworks.iotas.storage.impl.jdbc.provider.mysql.factory.MySqlExecutor;
import com.hortonworks.iotas.storage.impl.jdbc.provider.mysql.query.MySqlQueryUtils;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.query.SqlQuery;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.statement.PreparedStatementBuilder;
import com.hortonworks.iotas.test.IntegrationTest;
import org.h2.tools.RunScript;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

@Category(IntegrationTest.class)
public abstract class JdbcStorageManagerIntegrationTest extends AbstractStoreManagerTest {
    protected static StorageManager jdbcStorageManager;
    protected static Database database;
    protected static ConnectionBuilder connectionBuilder;

    protected enum Database {MYSQL, H2, PHOENIX}

    // ===== Tests Setup ====
    // Class level initialization is done in the implementing subclasses

    @Before
    public void setUp() throws Exception {
        createTables();
    }

    @After
    public void tearDown() throws Exception {
        jdbcStorageManager.cleanup();
        dropTables();
    }

    @Override
    protected StorageManager getStorageManager() {
        return jdbcStorageManager;
    }

    @Override
    protected void setStorableTests() {
        storableTests = new ArrayList<StorableTest>() {{
            add(new DataSourceTest());
            add(new DeviceJdbcTest());
            add(new ParsersTest());
            add(new DataFeedsJdbcTest());
        }};
    }

    // =============== TEST METHODS ===============

    @Test
    public void testList_EmptyDb_EmptyCollection() {
        for (StorableTest test : storableTests) {
            Collection<Storable> found = getStorageManager().list(test.getStorableList().get(0).getStorableKey().getNameSpace());
            Assert.assertNotNull(found);
            Assert.assertTrue(found.isEmpty());
        }
    }

    @Test(expected = NonIncrementalColumnException.class)
    public void testNextId_NoAutoincrementTable_NonIncrementalKeyException() throws Exception {
        for (StorableTest test : storableTests) {
            if (test instanceof DeviceTest) {
                    getStorageManager().nextId(test.getNameSpace());    // should throw exception
            }
        }
    }

    @Test
    public void testNextId_AutoincrementColumn_IdPlusOne() throws Exception {

        for (StorableTest test : storableTests) {
            // Device does not have auto_increment, and therefore there is no concept of nextId and should throw exception (tested below)
            if (!(test instanceof DeviceTest)) {
                doTestNextId_AutoincrementColumn_IdPlusOne(test);
            }
        }
    }

    // ============= Inner classes that handle the initialization steps required for the Storable entity to be tested =================

    //Device has foreign key in DataSource table, which has to be initialized before we can insert data in the Device table
    class DeviceJdbcTest extends DeviceTest {
        @Override
        public void init() {
            new DataSourceTest().addAllToStorage();
        }
    }

    class DataFeedsJdbcTest extends DataFeedsTest {
        // DataFeed has foreign keys in ParserInfo and DataSource tables, which have to be
        // initialized before we can insert data in the DataFeed table
        @Override
        public void init() {
            new ParsersTest().addAllToStorage();
            new DataSourceTest().addAllToStorage();
        }
    }

    protected static Connection getConnection() {
        Connection connection = connectionBuilder.getConnection();
        log.debug("Opened connection {}", connection);
        return connection;
    }

    protected void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                log.debug("Closed connection {}", connection);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to close connection", e);
            }
        }
    }

    // ========= Class that overrides the getNextId() method to allow testing using MySql and H2 databases ==========

    protected static class MySqlExecutorForTest extends MySqlExecutor {
        public MySqlExecutorForTest(ExecutionConfig config, ConnectionBuilder connectionBuilder) {
            super(config, connectionBuilder);
        }

        public MySqlExecutorForTest(CacheBuilder<SqlQuery, PreparedStatementBuilder> cacheBuilder,
            ExecutionConfig config, ConnectionBuilder connectionBuilder) {
            super(config, connectionBuilder, cacheBuilder);
        }

        @Override
        protected Long getNextId(Connection connection, String namespace) throws SQLException {
            if (database.equals(Database.MYSQL)) {
                return super.nextId(namespace);
            } else {
                return MySqlQueryUtils.nextIdH2(connection, namespace, getConfig().getQueryTimeoutSecs());
            }
        }
    }


    // ========= Private helper methods  ==========

    private void createTables() throws SQLException, IOException {
        runScript("mysql/create_tables.sql");
    }

    private void dropTables() throws SQLException, IOException {
        runScript("mysql/drop_tables.sql");
    }

    private void runScript(String fileName) throws SQLException, IOException {
        Connection connection = null;
        try {
            connection = getConnection();
            RunScript.execute(connection, load(fileName));
        } finally {
            // We need to close the connection because H2 DB running in memory only allows one connection at a time
            closeConnection(connection);
        }
    }

    private Reader load(String fileName) throws IOException {
        return new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(fileName));
    }
}
