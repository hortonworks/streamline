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
import com.hortonworks.streamline.storage.exception.StorageException;
import com.hortonworks.streamline.storage.impl.jdbc.JdbcStorageManager;
import com.hortonworks.streamline.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.streamline.storage.impl.jdbc.config.HikariBasicConfig;
import com.hortonworks.streamline.storage.impl.jdbc.connection.HikariCPConnectionBuilder;
import com.hortonworks.streamline.storage.impl.jdbc.provider.mysql.factory.MySqlExecutor;
import com.hortonworks.streamline.storage.util.StorageUtils;
import org.apache.commons.io.IOUtils;
import org.h2.tools.RunScript;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class DbFileStorageTest {
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

    @After
    public void tearDown() throws Exception {
        runScript("drop_fileblob.sql");
    }

    @Test
    public void testUploadDownload() throws Exception {
        try {
            transactionManager.beginTransaction(TransactionIsolation.SERIALIZABLE);
            String input = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream(FILE_NAME), "UTF-8");
            dbFileStorage.upload(IOUtils.toInputStream(input, "UTF-8"), FILE_NAME);
            InputStream is = dbFileStorage.download(FILE_NAME);
            String output = IOUtils.toString(is, "UTF-8");
            Assert.assertEquals(input, output);
            transactionManager.commitTransaction();
        } catch (Exception e) {
            transactionManager.rollbackTransaction();
            throw e;
        }
    }

    @Test
    public void testUpdate() throws Exception {
        try {
            transactionManager.beginTransaction(TransactionIsolation.SERIALIZABLE);
            String input = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream(FILE_NAME), "UTF-8");
            dbFileStorage.upload(IOUtils.toInputStream(input, "UTF-8"), FILE_NAME);
            String update = input + " new text";
            dbFileStorage.upload(IOUtils.toInputStream(update, "UTF-8"), FILE_NAME);
            InputStream is = dbFileStorage.download(FILE_NAME);
            String output = IOUtils.toString(is, "UTF-8");
            Assert.assertEquals(update, output);
            transactionManager.commitTransaction();
        } catch (Exception e) {
            transactionManager.rollbackTransaction();
            throw e;
        }
    }

    @Test
    public void testDelete() throws Exception {
        try {
            transactionManager.beginTransaction(TransactionIsolation.SERIALIZABLE);
            String input = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream(FILE_NAME), "UTF-8");
            dbFileStorage.upload(IOUtils.toInputStream(input, "UTF-8"), FILE_NAME);
            Assert.assertTrue(dbFileStorage.exists(FILE_NAME));
            dbFileStorage.delete(FILE_NAME);
            Assert.assertFalse(dbFileStorage.exists(FILE_NAME));
            try {
                dbFileStorage.download(FILE_NAME);
                Assert.fail("Expected IOException in download after delete");
            } catch (IOException ex) {
            }
            transactionManager.commitTransaction();
        } catch (Exception e) {
            transactionManager.rollbackTransaction();
            throw e;
        }
    }

    @Test (expected = StorageException.class)
    public void testConcurrentUpload() throws Throwable {
        try {
            transactionManager.beginTransaction(TransactionIsolation.SERIALIZABLE);
            String input = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream(FILE_NAME), "UTF-8");
            String updated = input + " new text";
            dbFileStorage.upload(IOUtils.toInputStream(input, "UTF-8"), FILE_NAME);
            InputStream slowStream = new InputStream() {
                byte[] bytes = updated.getBytes("UTF-8");
                int i = 0;

                @Override
                public int read() throws IOException {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                    }
                    return (i < bytes.length) ? (bytes[i++] & 0xff) : -1;
                }
            };
            FutureTask<String> ft1 = new FutureTask<>(() -> {
                try {
                    transactionManager.beginTransaction(TransactionIsolation.SERIALIZABLE);
                    String name = dbFileStorage.upload(slowStream, FILE_NAME);
                    transactionManager.commitTransaction();
                    return name;
                } catch (Exception e) {
                    transactionManager.rollbackTransaction();
                    throw e;
                }
            });
            FutureTask<String> ft2 = new FutureTask<>(() -> {
                try {
                    transactionManager.beginTransaction(TransactionIsolation.SERIALIZABLE);
                    String name = dbFileStorage.upload(IOUtils.toInputStream(updated, "UTF-8"), FILE_NAME);
                    transactionManager.commitTransaction();
                    return name;
                } catch (Exception e) {
                    transactionManager.rollbackTransaction();
                    throw e;
                }
            });
            Thread t1 = new Thread(ft1);
            Thread t2 = new Thread(ft2);
            t1.start();
            t2.start();
            t1.join();
            t2.join();
            try {
                ft1.get();
            } catch (ExecutionException ex) {
                throw ex.getCause();
            }
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
