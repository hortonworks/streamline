/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hortonworks.iotas.storage.impl.jdbc.provider.sql.query;

import com.hortonworks.iotas.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.statement.PreparedStatementBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Static stateless class that provides useful metadata information
 */
public class MetadataHelper {
    private static final Logger log = LoggerFactory.getLogger(MetadataHelper.class);

    public static boolean isAutoIncrement(Connection connection, String namespace, int queryTimeoutSecs) throws SQLException {
        final ResultSetMetaData rsMetadata = new PreparedStatementBuilder(connection, new ExecutionConfig(queryTimeoutSecs),
                new SqlSelectQuery(namespace)).getMetaData();

        final int columnCount = rsMetadata.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            if (rsMetadata.isAutoIncrement(i)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isColumnInNamespace(Connection connection, int queryTimeoutSecs, String namespace, String columnName) throws SQLException {
        final ResultSetMetaData rsMetadata = new PreparedStatementBuilder(connection, new ExecutionConfig(queryTimeoutSecs),
                new SqlSelectQuery(namespace)).getMetaData();

        final int columnCount = rsMetadata.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            if (rsMetadata.getColumnName(i).equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
    }

}
