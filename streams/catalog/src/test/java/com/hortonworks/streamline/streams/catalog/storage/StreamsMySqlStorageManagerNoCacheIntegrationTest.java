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

import com.hortonworks.streamline.common.test.IntegrationTest;
import com.hortonworks.streamline.storage.impl.jdbc.JdbcStorageManager;
import com.hortonworks.streamline.storage.impl.jdbc.mysql.MySqlStorageManagerNoCacheIntegrationTest;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.factory.QueryExecutor;
import com.hortonworks.streamline.storage.util.StorageUtils;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public class StreamsMySqlStorageManagerNoCacheIntegrationTest extends MySqlStorageManagerNoCacheIntegrationTest {

    @Override
    protected void setStorableTests() {
        storableTests = new StreamCatagoryTests().getAllTests();
    }

    public JdbcStorageManager createJdbcStorageManager(QueryExecutor queryExecutor) {
        JdbcStorageManager jdbcStorageManager = new JdbcStorageManager(queryExecutor);
        jdbcStorageManager.registerStorables(StorageUtils.getStreamlineEntities());
        return jdbcStorageManager;
    }
}
