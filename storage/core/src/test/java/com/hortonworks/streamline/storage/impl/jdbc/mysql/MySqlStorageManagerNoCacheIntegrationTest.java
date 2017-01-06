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

package com.hortonworks.streamline.storage.impl.jdbc.mysql;

import com.hortonworks.streamline.common.test.IntegrationTest;
import com.hortonworks.streamline.storage.impl.jdbc.JdbcStorageManagerIntegrationTest;
import com.hortonworks.streamline.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.streamline.storage.impl.jdbc.config.HikariBasicConfig;
import com.hortonworks.streamline.storage.impl.jdbc.connection.ConnectionBuilder;
import com.hortonworks.streamline.storage.impl.jdbc.connection.HikariCPConnectionBuilder;
import com.hortonworks.streamline.storage.impl.jdbc.provider.mysql.factory.MySqlExecutor;
import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public abstract class MySqlStorageManagerNoCacheIntegrationTest extends JdbcStorageManagerIntegrationTest {

    public MySqlStorageManagerNoCacheIntegrationTest() {
        //MySql DB Configuration. Useful for local testing
        //setFields(new HikariCPConnectionBuilder(HikariBasicConfig.getMySqlHikariConfig()), Database.MYSQL);
        //H2 DB Configuration. Useful for testing as part of the build
        setFields(new HikariCPConnectionBuilder(HikariBasicConfig.getH2HikariConfig()), Database.H2);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }


    private void setFields(ConnectionBuilder connectionBuilder, Database db) {
        JdbcStorageManagerIntegrationTest.connectionBuilder = connectionBuilder;
        jdbcStorageManager = createJdbcStorageManager(new MySqlExecutor(new ExecutionConfig(-1), connectionBuilder));
        database = db;
    }
}
