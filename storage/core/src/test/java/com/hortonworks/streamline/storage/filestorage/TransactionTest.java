/**
 * Copyright 2017 Hortonworks.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.hortonworks.streamline.storage.filestorage;

import com.hortonworks.streamline.common.transaction.TransactionIsolation;
import com.hortonworks.streamline.storage.StorageManager;
import com.hortonworks.streamline.storage.TransactionManager;
import com.hortonworks.streamline.storage.impl.jdbc.JdbcStorageManager;
import com.hortonworks.streamline.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.streamline.storage.impl.jdbc.config.HikariBasicConfig;
import com.hortonworks.streamline.storage.impl.jdbc.connection.HikariCPConnectionBuilder;
import com.hortonworks.streamline.storage.impl.jdbc.provider.mysql.factory.MySqlExecutor;
import com.hortonworks.streamline.storage.util.StorageUtils;
import org.apache.commons.io.IOUtils;
import org.h2.tools.RunScript;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;

public class TransactionTest {

    private static final String FILE_NAME = "data.txt";
    DbFileStorage dbFileStorage;
    TransactionManager transactionManager;
    HikariCPConnectionBuilder connectionBuilder;

    @Before
    public void setUp() throws Exception {
        connectionBuilder = new HikariCPConnectionBuilder(HikariBasicConfig.getH2HikariConfig());
        MySqlExecutor queryExecutor = new MySqlExecutor(new ExecutionConfig(-1), connectionBuilder);
        StorageManager jdbcStorageManager = new JdbcStorageManager(queryExecutor);
        transactionManager = (TransactionManager) jdbcStorageManager;
        jdbcStorageManager.registerStorables(StorageUtils.getStorableEntities());
        dbFileStorage = new DbFileStorage();
        dbFileStorage.setStorageManager(jdbcStorageManager);
        runScript("create_fileblob.sql");
    }

    @Test
    public void testRollback() throws Exception {
        String input;

        try {
            transactionManager.beginTransaction(TransactionIsolation.SERIALIZABLE);
            input = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream(FILE_NAME), "UTF-8");
            dbFileStorage.upload(IOUtils.toInputStream(input, "UTF-8"), FILE_NAME);
            transactionManager.commitTransaction();
        } catch (Exception e) {
            transactionManager.rollbackTransaction();
            throw e;
        }

        try{
            transactionManager.beginTransaction(TransactionIsolation.SERIALIZABLE);
            String update = input + " new text";
            dbFileStorage.upload(IOUtils.toInputStream(update, "UTF-8"), FILE_NAME);
            InputStream is = dbFileStorage.download(FILE_NAME);
            String output = IOUtils.toString(is, "UTF-8");
            Assert.assertEquals(update, output);
            throw new Exception();
        } catch (Exception e) {
            transactionManager.rollbackTransaction();
        }

        try {
            transactionManager.beginTransaction(TransactionIsolation.SERIALIZABLE);
            InputStream is = dbFileStorage.download(FILE_NAME);
            String output = IOUtils.toString(is, "UTF-8");
            Assert.assertEquals(input, output);
            transactionManager.commitTransaction();
        } catch (Exception e) {
            transactionManager.rollbackTransaction();
            throw e;
        }
    }

    private void runScript(String fileName) throws SQLException, IOException {
        Connection connection = null;
        try {
            connection = connectionBuilder.getConnection();
            RunScript.execute(connection, load(fileName));
        } finally {
            connection.close();
        }
    }

    private Reader load(String fileName) throws IOException {
        return new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(fileName));
    }
}
