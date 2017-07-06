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
import com.hortonworks.streamline.common.function.SupplierException;
import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.catalog.exception.EntityNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ServiceComponentNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ServiceConfigurationNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ServiceNotFoundException;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ComponentPropertyPattern;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.EnvironmentServiceUtil;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.OverrideHadoopConfiguration;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.HBaseNamespaces;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Keytabs;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Principals;
import com.hortonworks.streamline.streams.cluster.service.metadata.json.Tables;
import com.hortonworks.streamline.streams.security.SecurityUtil;

import org.apache.commons.math3.util.Pair;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.security.User;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.PrivilegedActionException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Provides HBase databases tables metadata information using {@link org.apache.hadoop.hbase.client.HBaseAdmin}
 */
public class HBaseMetadataService implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(HBaseMetadataService.class);

    public static final int SAM_MAX_HBASE_CLIENT_RETRIES_NUMBER = 3;

    private static final String PROP_HBASE_MASTER_KEYTAB_FILE = "hbase.master.keytab.file";
    private static final String PROP_HBASE_MASTER_KERBEROS_PRINCIPAL = "hbase.master.kerberos.principal";

    private static final String AMBARI_JSON_SERVICE_HBASE = ServiceConfigurations.HBASE.name();
    private static final String AMBARI_JSON_COMPONENT_HBASE_MASTER = ComponentPropertyPattern.HBASE_MASTER.name();
    private static final List<String> AMBARI_JSON_CONFIG_HBASE_SITE =
            ImmutableList.copyOf(new String[] {ServiceConfigurations.HBASE.getConfNames()[2]});

    private final Admin hBaseAdmin;
    private final SecurityContext securityContext;
    private final Subject subject;
    private final User user;
    private final Component hbaseMaster;
    private final Collection<ComponentProcess> hbaseMasterProcesses;

    public HBaseMetadataService(Admin hBaseAdmin) {
        this(hBaseAdmin, null, null, null, null, null);
    }

    public HBaseMetadataService(Admin hBaseAdmin, SecurityContext securityContext,
            Subject subject, User user, Component hbaseMaster, Collection<ComponentProcess> hbaseMasterProcesses) {
        this.hBaseAdmin = hBaseAdmin;
        this.securityContext = securityContext;
        this.subject = subject;
        this.user = user;
        this.hbaseMaster = hbaseMaster;
        this.hbaseMasterProcesses = hbaseMasterProcesses;
        LOG.info("Created {}", this);
    }

    /**
     * Creates insecure {@link HBaseMetadataService} which delegates to {@link Admin} instantiated with default
     * {@link HBaseConfiguration} and {@code hbase-site.xml} properties overridden with the config
     * for the cluster imported in the service pool (either manually or using Ambari)
     */
    public static HBaseMetadataService newInstance(EnvironmentService environmentService, Long clusterId)
            throws IOException, EntityNotFoundException {

        return newInstance(overrideConfig(HBaseConfiguration.create(), environmentService, clusterId));
    }

    /**
     * Creates insecure {@link HBaseMetadataService} which delegates to {@link Admin}
     * instantiated with with the {@link Configuration} provided using the first parameter
     */
    public static HBaseMetadataService newInstance(Configuration hbaseConfig) throws IOException, EntityNotFoundException {
        return new HBaseMetadataService(ConnectionFactory.createConnection(hbaseConfig).getAdmin());
    }

    /**
     * Creates secure {@link HBaseMetadataService} which delegates to {@link Admin} instantiated with default
     * {@link HBaseConfiguration} and {@code hbase-site.xml} properties overridden with the config
     * for the cluster imported in the service pool (either manually or using Ambari)
     */
    public static HBaseMetadataService newInstance(EnvironmentService environmentService, Long clusterId,
            SecurityContext securityContext, Subject subject) throws IOException, EntityNotFoundException {

            return newInstance(
                    overrideConfig(HBaseConfiguration.create(), environmentService, clusterId),
                    securityContext, subject, getHbaseMasterComponent(environmentService, clusterId),
                    getHbaseMasterComponentProcesses(environmentService, clusterId));
    }

    /**
     * Creates secure {@link HBaseMetadataService} which delegates to {@link Admin}
     * instantiated with with the {@link Configuration} provided using the first parameter
     */
    public static HBaseMetadataService newInstance(Configuration hbaseConfig,
            SecurityContext securityContext, Subject subject, Component hbaseMaster, Collection<ComponentProcess> hbaseMasters)
                throws IOException, EntityNotFoundException {

        if (SecurityUtil.isKerberosAuthenticated(securityContext)) {
            UserGroupInformation.setConfiguration(hbaseConfig);                                             // Sets Kerberos rules
            final UserGroupInformation ugiFromSubject = UserGroupInformation.getUGIFromSubject(subject);    // Adds User principal to the subject
            final UserGroupInformation proxyUserForImpersonation = UserGroupInformation
                    .createProxyUser(securityContext.getUserPrincipal().getName(), ugiFromSubject);
            final User user = User.create(proxyUserForImpersonation);

            return new HBaseMetadataService(ConnectionFactory.createConnection(hbaseConfig, user)
                    .getAdmin(), securityContext, subject, user, hbaseMaster, hbaseMasters);
        } else {
            return newInstance(hbaseConfig);
        }
    }

    private static Component getHbaseMasterComponent(EnvironmentService environmentService, Long clusterId)
            throws ServiceNotFoundException, ServiceComponentNotFoundException {

        return EnvironmentServiceUtil.getComponent(
                environmentService, clusterId, AMBARI_JSON_SERVICE_HBASE, AMBARI_JSON_COMPONENT_HBASE_MASTER);
    }

    private static Collection<ComponentProcess> getHbaseMasterComponentProcesses(EnvironmentService environmentService, Long clusterId)
            throws ServiceNotFoundException, ServiceComponentNotFoundException {
        return EnvironmentServiceUtil.getComponentProcesses(
                environmentService, clusterId, AMBARI_JSON_SERVICE_HBASE, AMBARI_JSON_COMPONENT_HBASE_MASTER);
    }

    /**
     * @return All tables for all namespaces
     */
    public Tables getHBaseTables() throws Exception {
        final TableName[] tableNames = executeSecure(() -> hBaseAdmin.listTableNames());
        LOG.debug("HBase tables {}", Arrays.toString(tableNames));
        return Tables.newInstance(tableNames, securityContext, true, getPrincipals(), getKeytabs());
    }

    /**
     * @param namespace Namespace for which to get table names
     * @return All tables for the namespace given as parameter
     */
    public Tables getHBaseTables(final String namespace) throws IOException, PrivilegedActionException, InterruptedException {
        final TableName[] tableNames = executeSecure(() -> hBaseAdmin.listTableNamesByNamespace(namespace));
        LOG.debug("HBase namespace [{}] has tables {}", namespace, Arrays.toString(tableNames));
        return Tables.newInstance(tableNames, securityContext, true, getPrincipals(), getKeytabs());
    }

    /**
     * @return All namespaces
     */
    public HBaseNamespaces getHBaseNamespaces() throws IOException, PrivilegedActionException, InterruptedException {
        final HBaseNamespaces namespaces = HBaseNamespaces.newInstance(
                executeSecure(() -> hBaseAdmin.listNamespaceDescriptors()), securityContext, true, getPrincipals(), getKeytabs());
        LOG.debug("HBase namespaces {}", namespaces);
        return namespaces;
    }
    
    public Keytabs getKeytabs() throws InterruptedException, IOException, PrivilegedActionException {
        return executeSecure(() -> Keytabs.fromServiceProperties(hBaseAdmin.getConfiguration()
                .getValByRegex(PROP_HBASE_MASTER_KEYTAB_FILE)));
    }

    public Principals getPrincipals() throws InterruptedException, IOException, PrivilegedActionException {
        return executeSecure(() -> Principals.fromServiceProperties(hBaseAdmin.getConfiguration()
                .getValByRegex(PROP_HBASE_MASTER_KERBEROS_PRINCIPAL), new Pair<>(hbaseMaster, hbaseMasterProcesses)));
    }

    @Override
    public void close() throws Exception {
        executeSecure(() -> {
            final Connection connection = hBaseAdmin.getConnection();
            hBaseAdmin.close();
            connection.close();
            return null;
        });
    }

    private <T, E extends Exception> T executeSecure(SupplierException<T, E> action)
            throws PrivilegedActionException, E, IOException, InterruptedException {
        return SecurityUtil.execute(action, securityContext, user);
    }

    private static Configuration overrideConfig(Configuration hbaseConfig, EnvironmentService environmentService, Long clusterId)
            throws IOException, ServiceConfigurationNotFoundException, ServiceNotFoundException {
        return OverrideHadoopConfiguration.override(environmentService, clusterId,
                ServiceConfigurations.HBASE, AMBARI_JSON_CONFIG_HBASE_SITE, hbaseConfig, getMaxHBaseClientRetries());
    }

    public static Map<String, Function<String, String>> getMaxHBaseClientRetries() {
        return new HashMap<String, Function<String, String>>(){{
            put(HConstants.HBASE_CLIENT_RETRIES_NUMBER, (propVal) ->
                String.valueOf(Math.min(SAM_MAX_HBASE_CLIENT_RETRIES_NUMBER, Integer.valueOf(propVal))));
        }};
    }

    /*
       Create and delete methods useful for system tests. Left as package protected for now.
       These methods can be made public and exposed in REST API.
    */
    void createNamespace(String namespace) throws IOException {
        hBaseAdmin.createNamespace(NamespaceDescriptor.create(namespace).build());
    }

    void createTable(String namespace, String tableName, String familyName) throws IOException {
        hBaseAdmin.createTable(new HTableDescriptor(TableName.valueOf(namespace, tableName))
                .addFamily(new HColumnDescriptor(familyName)));
    }

    void deleteNamespace(String namespace) throws IOException {
        hBaseAdmin.deleteNamespace(namespace);
    }

    void deleteTable(String namespace, String tableName) throws IOException {
        hBaseAdmin.deleteTable(TableName.valueOf(namespace, tableName));
    }

    void disableTable(String namespace, String tableName) throws IOException {
        hBaseAdmin.disableTable(TableName.valueOf(namespace, tableName));
    }

    @Override
    public String toString() {
        return "HBaseMetadataService{" +
                "hBaseAdmin=" + hBaseAdmin +
                ", securityContext=" + securityContext +
                ", subject=" + subject +
                ", user=" + user +
                ", hbaseMaster=" + hbaseMaster +
                '}';
    }
}
