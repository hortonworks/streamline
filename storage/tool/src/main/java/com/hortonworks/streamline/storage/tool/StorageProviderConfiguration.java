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
package com.hortonworks.streamline.storage.tool;

public class StorageProviderConfiguration {
    private static final String MYSQL_DELIMITER = "#";
    private static final String POSTGRESQL_DELIMITER = "#";
    private static final String MYSQL = "mysql";
    private static final String POSTGRESQL = "postgresql";
    private static final String ORACLE = "oracle";
    private static final String ORACLE_DELIMITER = "#";

    private String driverClass;
    private String url;
    private String user = "";
    private String password = "";
    private String dbType;
    private String delimiter;

    private StorageProviderConfiguration(String driverClass, String url, String user, String password, String dbType,
                                         String delimiter) {
        this.driverClass = driverClass;
        this.url = url;
        this.user = user;
        this.password = password;
        this.dbType = dbType;
        this.delimiter = delimiter;
    }


    public static StorageProviderConfiguration mysql(String driverClass, String url) {
        return new StorageProviderConfiguration(driverClass, url, "", "", MYSQL, MYSQL_DELIMITER);
    }

    public static StorageProviderConfiguration mysql(String driverClass, String url, String user, String password) {
        return new StorageProviderConfiguration(driverClass, url, user, password, MYSQL, MYSQL_DELIMITER);
    }

    public static StorageProviderConfiguration postgresql(String driverClass, String url, String user, String password) {
        return new StorageProviderConfiguration(driverClass, url, user, password, POSTGRESQL, POSTGRESQL_DELIMITER);
    }

    public static StorageProviderConfiguration oracle(String driverClass, String url, String user, String password) {
        return new StorageProviderConfiguration(driverClass, url, user, password, ORACLE, ORACLE_DELIMITER);
    }

    public String getDriverClass() {
        return driverClass;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDbType() {
        return dbType;
    }

    public String getDelimiter() {
        return delimiter;
    }

}