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

package com.hortonworks.streamline.storage.tool.sql;

public enum JdbcDriverClass {

    MYSQL("com.mysql.jdbc.Driver"),
    POSTGRES("org.postgresql.Driver"),
    ORACLE("oracle.jdbc.driver.OracleDriver");

    private final String value;

    JdbcDriverClass(String className) {
        value = className;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static JdbcDriverClass fromDatabaseType(DatabaseType databaseType) {
        for (JdbcDriverClass jdbcDriverClass : values()) {
            if (jdbcDriverClass.name().equals(databaseType.name()))
                return jdbcDriverClass;
        }
        throw new IllegalArgumentException("Unknown Database Type : " + databaseType);
    }
}
