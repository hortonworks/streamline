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

import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import com.hortonworks.streamline.streams.catalog.exception.ServiceConfigurationNotFoundException;
import com.hortonworks.streamline.streams.catalog.exception.ServiceNotFoundException;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.OverrideHadoopConfiguration;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.Tables;
import com.hortonworks.streamline.streams.cluster.discovery.ambari.ServiceConfigurations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides HBase databases tables metadata information using {@link org.apache.hadoop.hbase.client.HBaseAdmin}
 */
public class HBaseMetadataService implements AutoCloseable {
    private static final List<String> STREAMS_JSON_SCHEMA_CONFIG_HBASE_SITE =
            ImmutableList.copyOf(new String[] {ServiceConfigurations.HBASE.getConfNames()[2]});

    private Admin hBaseAdmin;

    public HBaseMetadataService(Admin hBaseAdmin) {
        this.hBaseAdmin = hBaseAdmin;
    }

    /**
     * Creates a new instance of {@link HBaseMetadataService} which delegates to {@link Admin} instantiated with default {@link
     * HBaseConfiguration} and {@code hbase-site.xml} config related properties overridden with the values set in the hbase-site
     * config serialized in "streams json"
     */
    public static HBaseMetadataService newInstance(EnvironmentService environmentService, Long clusterId)
            throws IOException, ServiceConfigurationNotFoundException, ServiceNotFoundException {

        return newInstance(HBaseConfiguration.create(), environmentService, clusterId);
    }

    /**
     * Creates a new instance of {@link HBaseMetadataService} which delegates to {@link Admin} instantiated  with the provided
     * {@link HBaseConfiguration} and {@code hbase-site.xml} config related properties overridden with the values set in the
     * hbase-site config serialized in "streams json"
     */
    public static HBaseMetadataService newInstance(Configuration hbaseConfig, EnvironmentService environmentService, Long clusterId)
            throws IOException, ServiceConfigurationNotFoundException, ServiceNotFoundException {

        return new HBaseMetadataService(ConnectionFactory.createConnection(
                OverrideHadoopConfiguration.override(environmentService, clusterId, ServiceConfigurations.HBASE,
                        STREAMS_JSON_SCHEMA_CONFIG_HBASE_SITE, hbaseConfig))
                .getAdmin());
    }

    /**
     * @return All tables for all namespaces
     */
    public Tables getHBaseTables() throws IOException {
        final TableName[] tableNames = hBaseAdmin.listTableNames();
        return Tables.newInstance(tableNames);
    }

    /**
     * @param namespace Namespace for which to get table names
     * @return All tables for the namespace given as parameter
     */
    public Tables getHBaseTables(String namespace) throws IOException {
        final TableName[] tableNames = hBaseAdmin.listTableNamesByNamespace(namespace);
        return Tables.newInstance(tableNames);
    }

    /**
     * @return All namespaces
     */
    public Namespaces getHBaseNamespaces() throws IOException {
        return Namespaces.newInstance(hBaseAdmin.listNamespaceDescriptors());
    }

    @Override
    public void close() throws Exception {
        final Connection connection = hBaseAdmin.getConnection();
        hBaseAdmin.close();
        connection.close();
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

    /**
     * Wrapper used to show proper JSON formatting
     */
    public static class Namespaces {
        private List<String> namespaces;

        public Namespaces(List<String> namespaces) {
            this.namespaces = namespaces;
        }

        public static Namespaces newInstance(NamespaceDescriptor[] namespaceDescriptors) {
            List<String> namespaces = Collections.emptyList();
            if (namespaceDescriptors != null) {
                namespaces = new ArrayList<>(namespaceDescriptors.length);
                for (NamespaceDescriptor namespace : namespaceDescriptors) {
                    namespaces.add(namespace.getName());
                }
            }
            return new Namespaces(namespaces);
        }

        public List<String> getNamespaces() {
            return namespaces;
        }
    }
}
