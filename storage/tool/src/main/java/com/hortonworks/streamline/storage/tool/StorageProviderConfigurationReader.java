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
package com.hortonworks.streamline.storage.tool;

import java.util.Map;

public class StorageProviderConfigurationReader {
    private static final String JDBC_STORAGE_MANAGER_CLASS = "com.hortonworks.streamline.storage.impl.jdbc.JdbcStorageManager";
    private static final String STORAGE_PROVIDER_CONFIGURATION = "storageProviderConfiguration";
    private static final String PROPERTIES = "properties";
    private static final String PROVIDER_CLASS = "providerClass";
    private static final String DB_TYPE = "db.type";
    private static final String DB_PROPERTIES = "db.properties";
    private static final String DATA_SOURCE_CLASS_NAME = "dataSourceClassName";
    private static final String DATA_SOURCE_URL = "dataSource.url";
    private static final String DATA_SOURCE_USER = "dataSource.user";
    private static final String DATA_SOURCE_PASSWORD = "dataSource.password";
    private static final String JDBC_URL = "jdbcUrl";
    private static final String JDBC_DRIVER_CLASS = "jdbcDriverClass";
    private static final String PHOENIX = "phoenix";
    private static final String MYSQL = "mysql";

    public StorageProviderConfiguration readStorageConfig(Map<String, Object> conf) {
        Map<String, Object> storageConf = (Map<String, Object>) conf.get(
                STORAGE_PROVIDER_CONFIGURATION);
        if (storageConf == null) {
            throw new RuntimeException("No storageProviderConfiguration in config file.");
        }

        String providerClass = (String) storageConf.get(PROVIDER_CLASS);
        if (!providerClass.equals(JDBC_STORAGE_MANAGER_CLASS)) {
            throw new RuntimeException("Not supported provider class.");
        }

        Map<String, Object> properties = (Map<String, Object>) storageConf.get(PROPERTIES);
        if (properties == null) {
            throw new RuntimeException("No properties presented to storageProviderConfiguration.");
        }

        String dbType = (String) properties.get(DB_TYPE);
        if (dbType == null) {
            throw new RuntimeException("No db.type presented to properties.");
        }

        switch (dbType) {
            case PHOENIX:
                return readPhoenixProperties((Map<String, Object>) properties.get(DB_PROPERTIES));

            case MYSQL:
                return readMySQLProperties((Map<String, Object>) properties.get(DB_PROPERTIES));

            default:
                throw new RuntimeException("Not supported DB type: " + dbType);
        }
    }

    /**
     * storageProviderConfiguration:
     *   providerClass: "com.hortonworks.streamline.storage.impl.jdbc.JdbcStorageManager"
     *   properties:
     *     db.type: "mysql"
     *     queryTimeoutInSecs: 30
     *     db.properties:
     *       dataSourceClassName: "com.mysql.jdbc.jdbc2.optional.MysqlDataSource"
     *       dataSource.url: "jdbc:mysql://localhost/test"
     */
    private static StorageProviderConfiguration readMySQLProperties(Map<String, Object> dbProperties) {
        String jdbcDriverClass = (String) dbProperties.get(DATA_SOURCE_CLASS_NAME);
        String jdbcUrl = (String) dbProperties.get(DATA_SOURCE_URL);
        String user = (String) dbProperties.getOrDefault(DATA_SOURCE_USER, "");
        String password = (String) dbProperties.getOrDefault(DATA_SOURCE_PASSWORD, "");

        return StorageProviderConfiguration.mysql(jdbcDriverClass, jdbcUrl, user, password);
    }

    /**
     * storageProviderConfiguration:
     *   providerClass: "com.hortonworks.streamline.storage.impl.jdbc.JdbcStorageManager"
     *   properties:
     *     db.type: "phoenix"
     *     queryTimeoutInSecs: 30
     *     db.properties:
     *       jdbcDriverClass: "org.apache.phoenix.jdbc.PhoenixDriver"
     *       jdbcUrl: "jdbc:phoenix:localhost:2181"
     */
    private static StorageProviderConfiguration readPhoenixProperties(Map<String, Object> dbProperties) {
        String jdbcDriverClass = (String) dbProperties.get(JDBC_DRIVER_CLASS);
        String jdbcUrl = (String) dbProperties.get(JDBC_URL);

        return StorageProviderConfiguration.phoenix(jdbcDriverClass, jdbcUrl);
    }

}
