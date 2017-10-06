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


package com.hortonworks.streamline.storage.impl.jdbc.provider.mysql.factory;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.streamline.storage.impl.jdbc.connection.ConnectionBuilder;
import com.hortonworks.streamline.storage.impl.jdbc.connection.HikariCPConnectionBuilder;
import com.hortonworks.streamline.storage.impl.jdbc.provider.mysql.query.MySqlInsertUpdateDuplicate;
import com.hortonworks.streamline.storage.impl.jdbc.provider.mysql.query.MysqlInsertQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.mysql.query.MysqlSelectQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.mysql.query.MysqlUpdateQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.factory.AbstractQueryExecutor;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.SqlInsertQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.SqlQuery;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.statement.PreparedStatementBuilder;
import com.hortonworks.streamline.storage.impl.jdbc.util.Util;
import com.zaxxer.hikari.HikariConfig;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * SQL query executor for MySQL DB.
 *
 * To issue the new ID to insert and get auto issued key in concurrent manner, MySqlExecutor utilizes MySQL's
 * auto increment feature and JDBC's getGeneratedKeys() which is described to MySQL connector doc:
 * https://dev.mysql.com/doc/connector-j/5.1/en/connector-j-usagenotes-last-insert-id.html
 *
 * If the value of id is null, we let MySQL issue new ID and get the new ID. If the value of id is not null, we just use that value.
 */
public class MySqlExecutor extends AbstractQueryExecutor {

    /**
     * @param config Object that contains arbitrary configuration that may be needed for any of the steps of the query execution process
     * @param connectionBuilder Object that establishes the connection to the database
     */
    public MySqlExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder) {
        super(config, connectionBuilder);
    }

    /**
     * @param config Object that contains arbitrary configuration that may be needed for any of the steps of the query execution process
     * @param connectionBuilder Object that establishes the connection to the database
     * @param cacheBuilder Guava cache configuration. The maximum number of entries in cache (open connections)
     *                     must not exceed the maximum number of open database connections allowed
     */
    public MySqlExecutor(ExecutionConfig config, ConnectionBuilder connectionBuilder, CacheBuilder<SqlQuery, PreparedStatementBuilder> cacheBuilder) {
        super(config, connectionBuilder, cacheBuilder);
    }

    // ============= Public API methods =============

    @Override
    public void insert(Storable storable) {
        insertOrUpdateWithUniqueId(storable, new MysqlInsertQuery(storable));
    }

    @Override
    public void insertOrUpdate(final Storable storable) {
        insertOrUpdateWithUniqueId(storable, new MySqlInsertUpdateDuplicate(storable));
    }

    @Override
    public int update(Storable storable) {
        return executeUpdate(new MysqlUpdateQuery(storable));
    }

    @Override
    public Long nextId(String namespace) {
        // We intentionally return null. Please refer the class javadoc for more details.
        return null;
    }

    @Override
    public <T extends Storable> Collection<T> select(String namespace) {
        return executeQuery(namespace, new MysqlSelectQuery(namespace));
    }

    @Override
    public <T extends Storable> Collection<T> select(StorableKey storableKey) {
        return executeQuery(storableKey.getNameSpace(), new MysqlSelectQuery(storableKey));
    }

    private void insertOrUpdateWithUniqueId(final Storable storable, final SqlQuery sqlQuery) {
        try {
            Long id = storable.getId();
            if (id == null) {
                id = executeUpdateWithReturningGeneratedKey(sqlQuery);
                storable.setId(id);
            } else {
                executeUpdate(sqlQuery);
            }
        } catch (UnsupportedOperationException e) {
            executeUpdate(sqlQuery);
        }
    }

}
