/**
 * Copyright 2016 Hortonworks.
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

package com.hortonworks.streamline.storage.impl.jdbc.provider.sql.factory;

import com.hortonworks.streamline.common.transaction.TransactionIsolation;
import com.hortonworks.streamline.storage.OrderByField;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.StorableFactory;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.exception.NonIncrementalColumnException;
import com.hortonworks.streamline.storage.exception.StorageException;
import com.hortonworks.streamline.storage.impl.jdbc.config.ExecutionConfig;
import com.hortonworks.streamline.storage.impl.jdbc.util.CaseAgnosticStringSet;
import com.hortonworks.streamline.storage.search.SearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

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
     * Updates the specified storable in the storage
     *
     * @return the number of rows updated
     */
    int update(Storable storable);

    /**
     * Deletes the specified {@link StorableKey} from storage
     */
    void delete(StorableKey storableKey);

    /**
     * @return all entries in the given namespace
     */
    <T extends Storable> Collection<T> select(String namespace);

    /**
     *
     * @param namespace
     * @param orderByFields
     * @param <T>
     * @return
     */
    <T extends Storable> Collection<T> select(String namespace, List<OrderByField> orderByFields);

    /**
     * @return all entries that match the specified {@link StorableKey}
     */
    <T extends Storable> Collection<T> select(StorableKey storableKey);

    /**
     *
     * @param storableKey
     * @param orderByFields
     * @param <T>
     * @return
     */
    <T extends Storable> Collection<T> select(StorableKey storableKey, List<OrderByField> orderByFields);


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

    //todo unify all other select methods with this method as they are kind of special cases of SearchQuery
    <T extends Storable> Collection<T> select(SearchQuery searchQuery);

    /**
     *  @return returns set of column names for a given table
     */
    CaseAgnosticStringSet getColumnNames(String namespace) throws SQLException;

    /**
     *  Begins the transaction
     */
    void beginTransaction(TransactionIsolation transactionIsolationLevel);


    /**
     *  Discards the changes made to the storage layer and reverts to the last committed point
     */
    void rollbackTransaction();


    /**
     *  Flushes the changes made to the storage layer
     */
    void commitTransaction();
}
