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
package com.hortonworks.streamline.streams.cluster.service.metadata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.Tables;

import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Order;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.security.auth.Subject;
import javax.ws.rs.core.SecurityContext;

import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;

@Ignore
@RunWith(JMockit.class)
public class HiveMetadataServiceTest {
    private static final List<String> HIVE_TEST_DATABASES = ImmutableList.copyOf(new String[]{"test_database_1", "test_database_2"});
    private static final List<String> HIVE_TEST_TABLES = ImmutableList.copyOf(new String[]{"test_table_1", "test_table_2"});
    private static final String HIVE_SITE_CONFIG = "metadata/hive-site.json";
    private static final String HIVE_METASTORE_SITE_CONFIG = "metadata/hivemetastore-site.json";

    private HiveMetadataService hiveService;

    @Mocked
    private EnvironmentService environmentService;
    @Mocked
    private ServiceConfiguration serviceConfiguration;
    @Mocked
    private SecurityContext securityContext;
    @Mocked
    private Subject subject;


    private void setUp() throws Exception {
        new Expectations() {{
            serviceConfiguration.getConfigurationMap(); result = getHiveConfigs();
        }};

        hiveService = HiveMetadataService.newInstance(environmentService, 1L, securityContext, subject);

        for (String database : HIVE_TEST_DATABASES) {
            hiveService.createDatabase(database, "desc", "/tmp/h", new HashMap<>());
            for (String table : HIVE_TEST_TABLES) {
                hiveService.createTable(table, database, "owner", (int)System.currentTimeMillis(), (int)System.currentTimeMillis(), 
                        10, newStorageDescriptor(), Collections.emptyList(), new HashMap<>(), "orig", "expanded", "EXTERNAL_TABLE");
            }
        }
    }

    private StorageDescriptor newStorageDescriptor() {
        return new StorageDescriptor(Lists.newArrayList(new FieldSchema("name", "string", "comment")), "/tmp/h",
                "org.apache.hadoop.mapred.TextInputFormat", "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat", false,
                1, new SerDeInfo(), Lists.newArrayList("col1"), Lists.newArrayList(new Order("col1", 1)), new HashMap<>());
    }

    private void tearDown() throws Exception {
        for (String database : HIVE_TEST_DATABASES) {
            for (String table : HIVE_TEST_TABLES) {
                hiveService.dropTable(database, table);
            }
            hiveService.dropDatabase(database);
        }
        hiveService.close();
    }

    /*
        Calling all the tests in one method because table creation during setup is quite expensive and needs to be done
        in the scope of the test because it depends on recorded expectations in order to abstract lots of initialization.
     */
    @Test
    public void test_getDatabase_getTables() throws Exception {
        setUp();
        try {
            test_getHiveDatabases();
            test_getHiveTablesForDatabase();
        } finally {
            tearDown();
        }
    }

    private void test_getHiveTablesForDatabase() throws Exception {
        for (String db : HIVE_TEST_DATABASES) {
            final Tables hiveTables = hiveService.getHiveTables(db);
            Assert.assertEquals(HIVE_TEST_TABLES,
                                hiveTables.getTables().stream().sorted(String::compareTo).collect(Collectors.toList()));
        }
    }

    private void test_getHiveDatabases() throws Exception {
        final HiveMetadataService.Databases hiveDatabases = hiveService.getHiveDatabases();
        Assert.assertTrue(hiveDatabases.list().containsAll(HIVE_TEST_DATABASES));
    }

    /**
     * Merges the the hive-site and hivemetastore-site configs in the same map
     */
    private Map<String, String> getHiveConfigs() throws IOException {
        final Map<String,String> configs = new ObjectMapper().readValue(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(HIVE_SITE_CONFIG), new TypeReference<Map<String, String>>() { });

        configs.putAll(new ObjectMapper().readValue(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(HIVE_METASTORE_SITE_CONFIG), new TypeReference<Map<String, String>>() { }));

        return configs;
    }
}