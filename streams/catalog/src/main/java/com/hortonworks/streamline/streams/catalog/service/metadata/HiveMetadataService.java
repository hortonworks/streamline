package org.apache.streamline.streams.catalog.service.metadata;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.streamline.streams.catalog.exception.ServiceConfigurationNotFoundException;
import org.apache.streamline.streams.catalog.exception.ServiceNotFoundException;
import org.apache.streamline.streams.catalog.service.EnvironmentService;
import org.apache.streamline.streams.catalog.service.metadata.common.OverrideHadoopConfiguration;
import org.apache.streamline.streams.catalog.service.metadata.common.Tables;
import org.apache.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Provides Hive databases and database tables metadata information using {@link HiveMetaStoreClient}
 */
public class HiveMetadataService implements AutoCloseable {
    protected static final Logger LOG = LoggerFactory.getLogger(HiveMetadataService.class);

    private static final String STREAMS_JSON_SCHEMA_CONFIG_HIVE_METASTORE_SITE = ServiceConfigurations.HIVE.getConfNames()[3];
    private static final String STREAMS_JSON_SCHEMA_CONFIG_HIVE_SITE = ServiceConfigurations.HIVE.getConfNames()[6];

    private final HiveConf hiveConf;  // HiveConf used to create HiveMetaStoreClient. If this class is created with the 1 parameter constructor, it is set to null
    private HiveMetaStoreClient metaStoreClient;

    public HiveMetadataService(HiveMetaStoreClient metaStoreClient) {
        this(metaStoreClient, null);
    }

    private HiveMetadataService(HiveMetaStoreClient metaStoreClient, HiveConf hiveConf) {
        this.metaStoreClient = metaStoreClient;
        this.hiveConf = hiveConf;
    }

    /**
     * Creates a new instance of {@link HiveMetadataService} which delegates to {@link HiveMetaStoreClient} instantiated with
     * default {@link HiveConf} and {@code hivemetastore-site.xml} config related properties overridden with the
     * values set in the hivemetastore-site config serialized in "streams json"
     */
    public static HiveMetadataService newInstance(EnvironmentService environmentService, Long clusterId)
            throws MetaException, IOException, ServiceConfigurationNotFoundException, ServiceNotFoundException {
        return newInstance(new HiveConf(), environmentService, clusterId);
    }


    /**
     * Creates a new instance of {@link HiveMetadataService} which delegates to {@link HiveMetaStoreClient} instantiated with
     * the provided {@link HiveConf} and {@code hivemetastore-site.xml} config related properties overridden with the
     * values set in the hivemetastore-site config serialized in "streams json"
     */
    public static HiveMetadataService newInstance(HiveConf hiveConf, EnvironmentService environmentService, Long clusterId)
            throws MetaException, IOException, ServiceConfigurationNotFoundException, ServiceNotFoundException {
        return new HiveMetadataService(new HiveMetaStoreClient(
                OverrideHadoopConfiguration.override(environmentService, clusterId, ServiceConfigurations.HIVE,
                        getConfigNames(), hiveConf)), hiveConf);
    }

    private static List<String> getConfigNames() {
        return Lists.newArrayList(STREAMS_JSON_SCHEMA_CONFIG_HIVE_METASTORE_SITE,
                STREAMS_JSON_SCHEMA_CONFIG_HIVE_SITE);
    }

    /**
     * @return The table names of for the database specified in the parameter
     */
    public Tables getHiveTables(String dbName) throws MetaException {
        return Tables.newInstance(metaStoreClient.getAllTables(dbName));
    }

    /**
     * @return The names of all databases in the MetaStore.
     */
    public Databases getHiveDatabases() throws MetaException {
        return Databases.newInstance(metaStoreClient.getAllDatabases());
    }

    @Override
    public void close() throws Exception {
        metaStoreClient.close();
    }

    /*
        Create and delete methods useful for system tests. Left as package protected for now.
        These methods can be made public and exposed in REST API.
    */

    void createDatabase(String dbName, String description, String locationUri, Map<String, String> parameters) throws TException {
        metaStoreClient.createDatabase(new Database(dbName,description, locationUri, parameters));
    }

    void dropDatabase(String dbName) throws TException {
        metaStoreClient.dropDatabase(dbName);
    }

    void createTable(String tableName,
                     String dbName,
                     String owner,
                     int createTime,
                     int lastAccessTime,
                     int retention,
                     StorageDescriptor sd,
                     List<FieldSchema> partitionKeys,
                     Map<String,String> parameters,
                     String viewOriginalText,
                     String viewExpandedText,
                     String tableType) throws TException {

        metaStoreClient.createTable(new Table(
                tableName,
                dbName,
                owner,
                createTime,
                lastAccessTime,
                retention,
                sd,
                partitionKeys,
                parameters,
                viewOriginalText,
                viewExpandedText,
                tableType));
    }

    void dropTable(String dbName, String tableName) throws TException {
        metaStoreClient.dropTable(dbName, tableName);
    }

    /**
     * @return The instance of the {@link HiveMetaStoreClient} used to retrieve Hive databases and tables metadata
     */
    public HiveMetaStoreClient getMetaStoreClient() {
        return metaStoreClient;
    }

    /**
     * @return a copy of the {@link HiveConf} used to configure the {@link HiveMetaStoreClient} instance created
     * using the factory methods. null if this object was initialized using the
     * {@link HiveMetadataService#HiveMetadataService(org.apache.hadoop.hive.metastore.HiveMetaStoreClient)} constructor
     */
    public HiveConf getHiveConfCopy() {
        return hiveConf == null ? null : new HiveConf(hiveConf);
    }

    /**
     * Wrapper used to show proper JSON formatting
     */
    public static class Databases {
        private List<String> databases;

        public Databases(List<String> databases) {
            this.databases = databases;
        }

        public static Databases newInstance(List<String> databases) {
            return databases == null ? new Databases(Collections.<String>emptyList()) : new Databases(databases);
        }

        @JsonProperty("databases")
        public List<String> asList() {
            return databases;
        }
    }
}
