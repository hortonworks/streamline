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


package com.hortonworks.streamline.storage.impl.jdbc.provider.sql.factory;

import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.StorableFactory;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.exception.NonIncrementalColumnException;
import com.hortonworks.streamline.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.streamline.storage.impl.jdbc.util.CaseAgnosticStringSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Exposes CRUD and other useful operations to the persistence storage
 */
public interface QueryExecutor {
    Logger log = LoggerFactory.getLogger(QueryExecutor.class);

    /**
     * Inserts the specified {@link Storable} in storage.
     */
    void insert(Storable storable);

    /**
     * Inserts or updates the specified {@link Storable} in storage
     */
    void insertOrUpdate(Storable storable);

    /**
     * Deletes the specified {@link StorableKey} from storage
     */
    void delete(StorableKey storableKey);

    /**
     * @return all entries in the given namespace
     */
    <T extends Storable> Collection<T> select(String namespace);

    /**
     * @return all entries that match the specified {@link StorableKey}
     */
    <T extends Storable> Collection<T> select(StorableKey storableKey);

    /**
     * @return The next available id for the autoincrement column in the specified {@code namespace}
     * @exception NonIncrementalColumnException if {@code namespace} has no autoincrement column
     *
     */
    Long nextId(String namespace);

    /**
     * @return an open connection to the underlying storage
     */
    Connection getConnection();

    void closeConnection(Connection connection);

    /**
     * cleanup
     */
    void cleanup();

    ExecutionConfig getConfig();

    void setStorableFactory(StorableFactory storableFactory);

    /**
     *  @return returns set of column names for a given table
     */
    CaseAgnosticStringSet getColumnNames(String namespace) throws SQLException;
}
