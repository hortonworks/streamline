/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.storage.impl.jdbc.provider.phoenix.query;

import com.hortonworks.iotas.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.iotas.storage.impl.jdbc.connection.ConnectionBuilder;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.query.AbstractSqlQuery;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.statement.PreparedStatementBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Query to get next sequence id in phoenix for a given name space.
 */
public class PhoenixSequenceIdQuery {

    private static final Logger log = LoggerFactory.getLogger(PhoenixSequenceIdQuery.class);
    private static final String ID = "id";
    private static final String VALUE = "value";
    private static final String SEQUENCE_TABLE = "sequence_table";
    private String namespace;
    private final ConnectionBuilder connectionBuilder;
    private final int queryTimeoutSecs;

    public PhoenixSequenceIdQuery(String namespace, ConnectionBuilder connectionBuilder, int queryTimeoutSecs) {
        this.namespace = namespace;
        this.connectionBuilder = connectionBuilder;
        this.queryTimeoutSecs = queryTimeoutSecs;
    }

    public Long getNextID() {
        // this is kind of work around as there is no direct support in phoenix to get next sequence-id without using any tables,
        // it involves 3 roundtrips to phoenix/hbase (inefficient but there is a limitation from phoenix!).
        // SEQUENCE can be used for such columns in UPSERT queries directly but to get a simple sequence-id involves all this.
        // create sequence for each namespace and insert it into with a value uuid.
        // get the id for inserted uuid.
        // delete that entry from the table.
        long nextId = 0;
        UUID uuid = UUID.randomUUID();
        PhoenixSqlQuery updateQuery = new PhoenixSqlQuery("UPSERT INTO " + SEQUENCE_TABLE + "(\""+ID+"\", \"" + namespace + "\") VALUES('" + uuid + "', NEXT VALUE FOR " + namespace + "_sequence)");
        PhoenixSqlQuery selectQuery = new PhoenixSqlQuery("SELECT \"" + namespace + "\" FROM " + SEQUENCE_TABLE + " WHERE \"" + ID + "\"='" + uuid + "'");
        PhoenixSqlQuery deleteQuery = new PhoenixSqlQuery("DELETE FROM " + SEQUENCE_TABLE + " WHERE \"id\"='" + uuid + "'");

        try (Connection connection = connectionBuilder.getConnection();) {
            int upsertResult = new PreparedStatementBuilder(connection, new ExecutionConfig(queryTimeoutSecs), updateQuery).getPreparedStatement(updateQuery).executeUpdate();
            log.debug("Query [{}] is executed and returns result with [{}]", updateQuery, upsertResult);

            ResultSet selectResultSet = new PreparedStatementBuilder(connection, new ExecutionConfig(queryTimeoutSecs), selectQuery).getPreparedStatement(selectQuery).executeQuery();
            if (selectResultSet.next()) {
                nextId = selectResultSet.getLong(namespace);
            } else {
                throw new RuntimeException("No sequence-id created for the current sequence of [" + namespace + "]");
            }
            log.debug("Generated sequence id [{}] for [{}]", nextId, namespace);
            int deleteResult = new PreparedStatementBuilder(connection, new ExecutionConfig(queryTimeoutSecs), deleteQuery).getPreparedStatement(deleteQuery).executeUpdate();
            if (deleteResult == 0) {
                log.error("Could not delete entry in " + SEQUENCE_TABLE + " for value [{}]", namespace, uuid);
            } else {
                log.debug("Deleted entry with id [{}] and value [{}] successfully",uuid, nextId);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return nextId;
    }

    static class PhoenixSqlQuery extends AbstractSqlQuery {

        public PhoenixSqlQuery(String sql) {
            this.sql = sql;
        }

        @Override
        protected void setParameterizedSql() {
        }
    }
}
