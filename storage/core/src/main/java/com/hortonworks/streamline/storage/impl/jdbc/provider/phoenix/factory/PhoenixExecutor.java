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
package com.hortonworks.streamline.storage.impl.jdbc.provider.phoenix.factory;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.streamline.storage.impl.jdbc.connection.ConnectionBuilder;
import com.hortonworks.streamline.storage.impl.jdbc.connection.HikariCPConnectionBuilder;
import com.hortonworks.streamline.storage.impl.jdbc.provider.phoenix.query.PhoenixDeleteQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.phoenix.query.PhoenixSelectQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.phoenix.query.PhoenixSequenceIdQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.phoenix.query.PhoenixUpsertQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.factory.AbstractQueryExecutor;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.SqlQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.statement.PreparedStatementBuilder;
import com.hortonworks.streamline.storage.impl.jdbc.util.Util;
import com.zaxxer.hikari.HikariConfig;

import java.util.Collection;
import java.util.Map;

/**
 * SQL query executor for Phoenix.
 *
 * Phoenix doesn't support auto_increment feature, as well as JDBC's getGeneratedKeys().
 * In order to get over, we take a workaround to {@link PhoenixSequenceIdQuery#getNextID()} which provides issuing ID
 * in safe way.
 *
 * If the value of id is null, we issue a new ID and set ID to entity. If the value of id is not null, we just use that value.
 */
public class PhoenixExecutor extends AbstractQueryExecutor {

    public PhoenixExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder) {
        super(config, connectionBuilder);
    }

    public PhoenixExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder, CacheBuilder<SqlQuery, PreparedStatementBuilder> cacheBuilder) {
        super(config, connectionBuilder, cacheBuilder);
    }

    @Override
    public void insert(Storable storable) {
        insertOrUpdate(storable);
    }

    @Override
    public void insertOrUpdate(Storable storable) {
        try {
            Long id = storable.getId();
            if (id == null) {
                id = nextId(storable.getNameSpace());
                storable.setId(id);
            }
        } catch (UnsupportedOperationException e) {
            // no-op
        }

        executeUpdate(new PhoenixUpsertQuery(storable));
    }

    @Override
    public <T extends Storable> Collection<T> select(String namespace) {
        return executeQuery(namespace, new PhoenixSelectQuery(namespace));
    }

    @Override
    public <T extends Storable> Collection<T> select(StorableKey storableKey) {
        return executeQuery(storableKey.getNameSpace(), new PhoenixSelectQuery(storableKey));
    }

    @Override
    public void delete(StorableKey storableKey) {
        executeUpdate(new PhoenixDeleteQuery(storableKey));
    }

    @Override
    public Long nextId(String namespace) {
        PhoenixSequenceIdQuery phoenixSequenceIdQuery = new PhoenixSequenceIdQuery(namespace, connectionBuilder, queryTimeoutSecs);
        return phoenixSequenceIdQuery.getNextID();
    }

    public static PhoenixExecutor createExecutor(Map<String, Object> jdbcProps) throws Exception {
        Util.validateJDBCProperties(jdbcProps, Lists.newArrayList("jdbcDriverClass", "jdbcUrl"));

        String driverClassName = (String) jdbcProps.get("jdbcDriverClass");
        log.info("jdbc driver class: [{}]", driverClassName);
        Class.forName(driverClassName);

        String jdbcUrl = (String) jdbcProps.get("jdbcUrl");
        log.info("jdbc url is: [{}] ", jdbcUrl);

        int queryTimeOutInSecs = -1;
        if(jdbcProps.containsKey("queryTimeoutInSecs")) {
            queryTimeOutInSecs = (Integer) jdbcProps.get("queryTimeoutInSecs");
            if(queryTimeOutInSecs < 0) {
                throw new IllegalArgumentException("queryTimeoutInSecs property can not be negative");
            }
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);

        final HikariCPConnectionBuilder connectionBuilder = new HikariCPConnectionBuilder(hikariConfig);
        final ExecutionConfig executionConfig = new ExecutionConfig(queryTimeOutInSecs);
        CacheBuilder cacheBuilder = null;
        if(jdbcProps.containsKey("cacheSize")) {
            cacheBuilder = CacheBuilder.newBuilder().maximumSize((Integer)jdbcProps.get("cacheSize"));
        }
        return new PhoenixExecutor(executionConfig, connectionBuilder, cacheBuilder);
    }

}
