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

package com.hortonworks.iotas.storage.impl.jdbc.mysql.query;

import com.hortonworks.iotas.storage.exception.NonIncrementalColumnException;
import com.hortonworks.iotas.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.iotas.storage.impl.jdbc.mysql.statement.PreparedStatementBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Static stateless class that provides useful metadata information */
public class MetadataHelper {
    private static final Logger log = LoggerFactory.getLogger(MetadataHelper.class);

    public static boolean isAutoIncrement(Connection connection, String namespace, int queryTimeoutSecs) throws SQLException {
        final ResultSetMetaData rsMetadata = new PreparedStatementBuilder(connection, new ExecutionConfig(queryTimeoutSecs),
                new MySqlSelect(namespace)).getMetaData();

        final int columnCount = rsMetadata.getColumnCount();

        for (int i = 1 ; i <= columnCount; i++) {
            if (rsMetadata.isAutoIncrement(i)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isColumnInNamespace(Connection connection, int queryTimeoutSecs, String namespace, String columnName) throws SQLException {
        final ResultSetMetaData rsMetadata = new PreparedStatementBuilder(connection, new ExecutionConfig(queryTimeoutSecs),
                new MySqlSelect(namespace)).getMetaData();

        final int columnCount = rsMetadata.getColumnCount();

        for (int i = 1 ; i <= columnCount; i++) {
            if (rsMetadata.getColumnName(i).equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
    }

    public static long nextIdMySql(Connection connection, String namespace, int queryTimeoutSecs) throws SQLException {
        if (!isAutoIncrement(connection, namespace, queryTimeoutSecs)) {
            throw new NonIncrementalColumnException();
        }

        final ResultSet resultSet = getResultSet(connection, queryTimeoutSecs, buildNextIdMySql(connection, namespace));
        resultSet.next();
        final long nextId = resultSet.getLong("AUTO_INCREMENT");
        log.debug("Next id for auto increment table [{}] = {}", namespace, nextId);
        return nextId;
    }

    private static ResultSet getResultSet(Connection connection, int queryTimeoutSecs, String sql) throws SQLException {
        final MySqlQuery sqlBuilder = new MySqlQuery(sql);
        return new PreparedStatementBuilder(connection, new ExecutionConfig(queryTimeoutSecs),
                sqlBuilder).getPreparedStatement(sqlBuilder).executeQuery();
    }

    private static String buildNextIdMySql(Connection connection, String namespace) throws SQLException {
        final String database  = connection.getCatalog();
        final String sql = "SELECT AUTO_INCREMENT FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '"
                + namespace + "' AND TABLE_SCHEMA = '" + database + "'";
        log.debug("nextIdMySql() SQL query: {}", sql);
        return sql;
    }

    public static long nextIdH2(Connection connection, String namespace, int queryTimeoutSecs) throws SQLException {
        if (!isAutoIncrement(connection, namespace, queryTimeoutSecs)) {
            throw new NonIncrementalColumnException();
        }

        final ResultSet resultSet = getResultSet(connection, queryTimeoutSecs,
                buildNextIdH2(getH2SequenceName(connection, namespace, queryTimeoutSecs)));

        resultSet.next();
        final long nextId = resultSet.getLong("CURRENT_VALUE");
        log.debug("Next id for auto increment table [{}] = {}", namespace, nextId);
        return nextId;
    }

    private static String buildNextIdH2(String sequenceName) throws SQLException {
        final String sql = "SELECT CURRENT_VALUE FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_NAME = '" + sequenceName + "'";
        log.debug("nextIdH2() SQL query: {}", sql);
        return sql;
    }

    private static String getH2SequenceName(Connection connection, String namespace, int queryTimeoutSecs) throws SQLException {
        final ResultSet resultSet = getResultSet(connection, queryTimeoutSecs, getH2InfoSchemaSql(namespace));

        resultSet.next();
        String sql = resultSet.getString("SQL");

        Pattern p = Pattern.compile("(?i)(SYSTEM_SEQUENCE.*?)([)])");
        Matcher m = p.matcher(sql);
        String seqName = null;
        if (m.find()) {
            seqName = m.group(1);
            log.debug("SQL: {} => \n\t", sql, seqName);
        }

        if (seqName == null) {
            throw new RuntimeException("No sequence name found for namespace [" + namespace + "]");
        }
        return seqName;
    }

    private static String getH2InfoSchemaSql(String namespace) {
        return "SELECT `SQL` FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + namespace + "'";
    }

}
