package org.apache.streamline.streams.catalog.service.metadata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Order;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.streamline.streams.catalog.ServiceConfiguration;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.catalog.service.metadata.common.Tables;
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
    private StreamCatalogService catalogService;
    @Mocked
    private ServiceConfiguration serviceConfiguration;

    private void setUp() throws Exception {
        new Expectations() {{
            serviceConfiguration.getConfigurationMap(); result = getHiveConfigs();
        }};

        hiveService = HiveMetadataService.newInstance(catalogService, 1L);

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
        Assert.assertTrue(hiveDatabases.asList().containsAll(HIVE_TEST_DATABASES));
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