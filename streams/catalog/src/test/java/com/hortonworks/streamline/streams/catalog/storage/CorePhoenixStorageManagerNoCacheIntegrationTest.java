/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.streams.catalog.storage;

import com.hortonworks.streamline.common.test.HBaseIntegrationTest;
import com.hortonworks.streamline.storage.StorableTest;
import com.hortonworks.streamline.storage.exception.NonIncrementalColumnException;
import com.hortonworks.streamline.storage.impl.jdbc.JdbcStorageManager;
import com.hortonworks.streamline.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.streamline.storage.impl.jdbc.phoenix.PhoenixStorageManagerNoCacheIntegrationTest;
import com.hortonworks.streamline.storage.impl.jdbc.provider.phoenix.factory.PhoenixExecutor;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.factory.QueryExecutor;
import com.hortonworks.streamline.storage.util.StorageUtils;
import com.hortonworks.streamline.streams.catalog.TopologyRule;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Phoenix storage integration tests without using cache.
 *
 */
@Category(HBaseIntegrationTest.class)
public  class CorePhoenixStorageManagerNoCacheIntegrationTest extends PhoenixStorageManagerNoCacheIntegrationTest {


    @Override
    protected void setStorableTests() {
        storableTests = new CatalogTests().getAllTests();
    }

    @Test
    public void testNextId_AutoincrementColumn_IdPlusOne() throws Exception {
        final PhoenixExecutor phoenixExecutor = new PhoenixExecutor(new ExecutionConfig(-1), connectionBuilder);
        String[] nameSpaces = {TopologyRule.NAMESPACE};
        for (String nameSpace : nameSpaces) {
            log.info("Generating sequence-ids for namespace: [{}]", nameSpace);
            for (int x = 0; x < 100; x++) {
                final Long nextId = phoenixExecutor.nextId(nameSpace);
                log.info("\t\t[{}]", nextId);
                Assert.assertTrue(nextId > 0);
            }
        }
    }

    public JdbcStorageManager createJdbcStorageManager(QueryExecutor queryExecutor) {
        JdbcStorageManager jdbcStorageManager = new JdbcStorageManager(queryExecutor);
        jdbcStorageManager.registerStorables(StorageUtils.getStreamlineEntities());
        return jdbcStorageManager;
    }

    @Test(expected = NonIncrementalColumnException.class)
    public void testNextId_NoAutoincrementTable_NonIncrementalKeyException() throws Exception {
        for (StorableTest test : storableTests) {
            if (test instanceof CatalogTests.FilesTest) {
                getStorageManager().nextId(test.getNameSpace());    // should throw exception
            }
        }
    }

}