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

import com.google.common.collect.Lists;

import com.hortonworks.streamline.common.function.SupplierException;
import com.hortonworks.streamline.streams.cluster.catalog.Component;
import com.hortonworks.streamline.streams.cluster.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.cluster.exception.EntityNotFoundException;
import com.hortonworks.streamline.streams.cluster.exception.ServiceComponentNotFoundException;
import com.hortonworks.streamline.streams.cluster.exception.ServiceNotFoundException;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.EnvironmentServiceUtil;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.OverrideHadoopConfiguration;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.HiveDatabases;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Keytabs;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Principals;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Tables;
import com.hortonworks.streamline.streams.security.SecurityUtil;

import org.apache.commons.math3.util.Pair;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.PrivilegedActionException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.ws.rs.core.SecurityContext;

/**
 * Provides Hive databases, and database tables metadata information using {@link HiveMetaStoreClient}
 */
public class HiveMetadataService implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(HiveMetadataService.class);

    private static final String PROP_HIVE_METASTORE_KERBEROS_KEYTAB_FILE = "hive.metastore.kerberos.keytab.file";
    private static final String PROP_HIVE_METASTORE_KERBEROS_PRINCIPAL = "hive.metastore.kerberos.principal";

    private static final String AMBARI_JSON_SERVICE_HIVE = ServiceConfigurations.HIVE.name();
    private static final String AMBARI_JSON_COMPONENT_HIVE_METASTORE = ComponentPropertyPattern.HIVE_METASTORE.name();

    private static final String AMBARI_JSON_CONFIG_HIVE_METASTORE_SITE = ServiceConfigurations.HIVE.getConfNames()[3];
    private static final String AMBARI_JSON_CONFIG_HIVE_SITE = ServiceConfigurations.HIVE.getConfNames()[6];

    private final HiveConf hiveConf;  // HiveConf used to create HiveMetaStoreClient. If this class is created with
                                      // the 3 parameter constructor, it is set to null
    private final HiveMetaStoreClient metaStoreClient;
    private final SecurityContext securityContext;
    private final Subject subject;
    private final Component hiveMetastore;
    private final Collection<ComponentProcess> hiveMetastoreProcesses;

    /**
     * @param hiveConf The hive configuration used to instantiate {@link HiveMetaStoreClient}
     */
    public HiveMetadataService(HiveMetaStoreClient metaStoreClient, HiveConf hiveConf,
                               SecurityContext securityContext, Subject subject, Component hiveMetastore,
                               Collection<ComponentProcess> hiveMetastoreProcesses) {
        this.metaStoreClient = metaStoreClient;
        this.hiveConf = hiveConf;
        this.securityContext = securityContext;
        this.subject = subject;
        this.hiveMetastore = hiveMetastore;
        this.hiveMetastoreProcesses = hiveMetastoreProcesses;
        LOG.info("Created {}", this);
    }

    /**
     * Creates secure {@link HiveMetadataService}, which delegates to {@link HiveMetaStoreClient} instantiated with
     * default {@link HiveConf}, and {@code hivemetastore-site.xml} and {@code hive-site.xml} properties overridden
     * with the config for the cluster imported in the service pool (either manually or using Ambari)
     */
    public static HiveMetadataService newInstance(EnvironmentService environmentService, Long clusterId,
                                                  SecurityContext securityContext, Subject subject)
            throws MetaException, IOException, EntityNotFoundException, PrivilegedActionException {

        return newInstance(overrideConfig(environmentService, clusterId),
                securityContext, subject, getHiveMetastoreComponent(environmentService, clusterId),
                getHiveMetastores(environmentService, clusterId));
    }

    private static HiveConf overrideConfig(EnvironmentService environmentService, Long clusterId)
            throws IOException, EntityNotFoundException {
        return OverrideHadoopConfiguration.override(environmentService, clusterId,
                ServiceConfigurations.HIVE, getConfigNames(), new HiveConf());
    }

    /**
     * Creates secure {@link HiveMetadataService}, which delegates to {@link HiveMetaStoreClient}
     * instantiated with the {@link HiveConf} provided using the first parameter
     */
    public static HiveMetadataService newInstance(HiveConf hiveConf, SecurityContext securityContext,
                                                  Subject subject, Component hiveMetastore,
                                                  Collection<ComponentProcess> hiveMetastoreProcesses)
                throws MetaException, IOException, EntityNotFoundException, PrivilegedActionException {

        if (SecurityUtil.isKerberosAuthenticated(securityContext)) {
            UserGroupInformation.setConfiguration(hiveConf);    // Sets Kerberos rules
            UserGroupInformation.getUGIFromSubject(subject);    // Adds User principal to this subject

            return new HiveMetadataService(
                    SecurityUtil.execute(() -> new HiveMetaStoreClient(hiveConf), securityContext, subject),
                        hiveConf, securityContext, subject, hiveMetastore, hiveMetastoreProcesses);
        } else {
            return new HiveMetadataService(new HiveMetaStoreClient(hiveConf), hiveConf, securityContext, subject,
                    hiveMetastore, hiveMetastoreProcesses);
        }
    }

    private static Component getHiveMetastoreComponent(EnvironmentService environmentService, Long clusterId)
            throws ServiceNotFoundException, ServiceComponentNotFoundException {

        return EnvironmentServiceUtil.getComponent(
                environmentService, clusterId, AMBARI_JSON_SERVICE_HIVE, AMBARI_JSON_COMPONENT_HIVE_METASTORE);
    }

    private static Collection<ComponentProcess> getHiveMetastores(EnvironmentService environmentService, Long clusterId)
            throws ServiceNotFoundException, ServiceComponentNotFoundException {
        return EnvironmentServiceUtil.getComponentProcesses(
                environmentService, clusterId, AMBARI_JSON_SERVICE_HIVE, AMBARI_JSON_COMPONENT_HIVE_METASTORE);
    }

    private static List<String> getConfigNames() {
        return Lists.newArrayList(AMBARI_JSON_CONFIG_HIVE_METASTORE_SITE,
                AMBARI_JSON_CONFIG_HIVE_SITE);
    }

    /**
     * @return The table names for the database specified in the parameter
     */
    public Tables getHiveTables(String dbName) throws  MetaException, PrivilegedActionException,
            IOException, InterruptedException {

        final Tables tables = Tables.newInstance(executeSecure(() -> metaStoreClient.getAllTables(dbName)),
                securityContext, false, getPrincipals(), getKeytabs());
        LOG.debug("Hive database [{}] has tables {}", dbName, tables.getTables());
        return tables;
    }

    /**
     * @return The names of all databases in the MetaStore.
     */
    public HiveDatabases getHiveDatabases() throws MetaException, PrivilegedActionException, IOException, InterruptedException {
        final HiveDatabases databases = HiveDatabases.newInstance(
                executeSecure(metaStoreClient::getAllDatabases), securityContext, getPrincipals(), getKeytabs());
        LOG.debug("Hive databases {}", databases.list());
        return databases;
    }

    public Keytabs getKeytabs() throws InterruptedException, IOException, PrivilegedActionException {
        return executeSecure(() -> Keytabs.fromServiceProperties(hiveConf.getValByRegex(PROP_HIVE_METASTORE_KERBEROS_KEYTAB_FILE)));
    }

    public Principals getPrincipals() throws InterruptedException, IOException, PrivilegedActionException {
        return executeSecure(() -> Principals.fromServiceProperties(
                hiveConf.getValByRegex(PROP_HIVE_METASTORE_KERBEROS_PRINCIPAL), new Pair<>(hiveMetastore, hiveMetastoreProcesses)));
    }

    @Override
    public void close() throws Exception {
        executeSecure(() -> {
            metaStoreClient.close();
            return null;
        });
    }

    private <T, E extends Exception> T executeSecure(SupplierException<T, E> action) throws PrivilegedActionException, E {
        return SecurityUtil.execute(action, securityContext, subject);
    }

    /*
     *   Create and delete methods useful for system tests. Left as package protected for now.
     *   These methods can be made public and exposed in REST API.
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
     * @return a copy of the {@link HiveConf} used to configure the {@link HiveMetaStoreClient}
     * instance created using the factory methods. */
    public HiveConf getHiveConfCopy() {
        return hiveConf == null ? null : new HiveConf(hiveConf);
    }

    @Override
    public String toString() {
        return "HiveMetadataService{" +
                "hiveConf=" + hiveConf +
                ", metaStoreClient=" + metaStoreClient +
                ", securityContext=" + securityContext +
                ", subject=" + subject +
                ", hiveMetastore=" + hiveMetastore +
                '}';
    }
}
