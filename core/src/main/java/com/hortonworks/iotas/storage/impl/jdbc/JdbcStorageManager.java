/**
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
package com.hortonworks.iotas.storage.impl.jdbc;


import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.service.CatalogService;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;
import com.hortonworks.iotas.storage.StorageManager;
import com.hortonworks.iotas.storage.exception.AlreadyExistsException;
import com.hortonworks.iotas.storage.exception.IllegalQueryParameterException;
import com.hortonworks.iotas.storage.exception.StorageException;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.factory.QueryExecutor;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.query.MetadataHelper;
import com.hortonworks.iotas.storage.impl.jdbc.provider.sql.query.SqlSelectQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: Need to assess synchronization
public class JdbcStorageManager implements StorageManager {
    private static final Logger log = LoggerFactory.getLogger(StorageManager.class);

    private final QueryExecutor queryExecutor;

    public JdbcStorageManager(QueryExecutor queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    @Override
    public void add(Storable storable) throws AlreadyExistsException {
        log.debug("Adding storable [{}]", storable);
        final Storable existing = get(storable.getStorableKey());

        if(existing == null) {
            addOrUpdate(storable);
        } else if (!existing.equals(storable)) {
            throw new AlreadyExistsException("Another instance with same id = " + storable.getPrimaryKey()
                    + " exists with different value in namespace " + storable.getNameSpace()
                    + ". Consider using addOrUpdate method if you always want to overwrite.");
        }
    }

    @Override
    public <T extends Storable> T remove(StorableKey key) throws StorageException {
        T oldVal = get(key);
        if (key != null) {
            log.debug("Removing storable key [{}]", key);
            queryExecutor.delete(key);
        }
        return oldVal;
    }

    @Override
    public void addOrUpdate(Storable storable) throws StorageException {
        log.debug("Adding or updating storable [{}]", storable);
        queryExecutor.insertOrUpdate(storable);
    }

    @Override
    public <T extends Storable> T get(StorableKey key) throws StorageException {
        log.debug("Searching entry for storable key [{}]", key);

        final Collection<T> entries = queryExecutor.select(key);
        T entry = null;
        if (entries.size() > 0) {
            if (entries.size() > 1) {
                log.debug("More than one entry found for storable key [{}]", key);
            }
            entry = entries.iterator().next();
        }
        log.debug("Querying key = [{}]\n\t returned [{}]", key, entry);
        return entry;
    }

    @Override
    public <T extends Storable> Collection<T> find(String namespace, List<CatalogService.QueryParam> queryParams)
            throws StorageException {
        log.debug("Searching for entries in table [{}] that match queryParams [{}]", namespace, queryParams);

        if (queryParams == null) {
            return list(namespace);
        }
        Collection<T> entries = Collections.EMPTY_LIST;

        try {
            StorableKey storableKey = buildStorableKey(namespace, queryParams);
            if (storableKey != null) {
                entries = queryExecutor.select(storableKey);
            }
        } catch (Exception e) {
            throw new StorageException(e);
        }
        log.debug("Querying table = [{}]\n\t filter = [{}]\n\t returned [{}]", namespace, queryParams, entries);
        return entries;
    }

    @Override
    public <T extends Storable> Collection<T> list(String namespace) throws StorageException {
        log.debug("Listing entries for table [{}]", namespace);
        final Collection<T> entries = queryExecutor.select(namespace);
        log.debug("Querying table = [{}]\n\t returned [{}]", namespace, entries);
        return entries;
    }

    @Override
    public void cleanup() throws StorageException {
        queryExecutor.cleanup();
    }

    @Override
    public Long nextId(String namespace) {
        log.debug("Finding nextId for table [{}]", namespace);
        // This only works if the table has auto-increment. The TABLE_SCHEMA part is implicitly specified in the Connection object
        // SELECT AUTO_INCREMENT FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'temp' AND TABLE_SCHEMA = 'test'
        return queryExecutor.nextId(namespace);
    }

    // private helper methods

    /**
     * Query parameters are typically specified for a column or key in a database table or storage namespace. Therefore, we build
     * the {@link StorableKey} from the list of query parameters, and then can use {@link SqlSelectQuery} builder to generate the query using
     * the query parameters in the where clause
     *
     * @return {@link StorableKey} with all query parameters that match database columns <br/>
     * null if none of the query parameters specified matches a column in the DB
     */
    private StorableKey buildStorableKey(String namespace, List<CatalogService.QueryParam> queryParams) throws Exception {
        final Map<Schema.Field, Object> fieldsToVal = new HashMap<>();
        final Connection connection = queryExecutor.getConnection();
        StorableKey storableKey = null;

        try {
            for (CatalogService.QueryParam qp : queryParams) {
                int queryTimeoutSecs = queryExecutor.getConfig().getQueryTimeoutSecs();
                if (!MetadataHelper.isColumnInNamespace(connection, queryTimeoutSecs, namespace, qp.getName())) {
                    log.warn("Query parameter [{}] does not exist for namespace [{}]. Query parameter ignored.", qp.getName(), namespace);
                } else {
                    final String val = qp.getValue();
                    final Schema.Type typeOfVal = Schema.Type.getTypeOfVal(val);
                    fieldsToVal.put(new Schema.Field(qp.getName(), typeOfVal),
                        typeOfVal.getJavaType().getConstructor(String.class).newInstance(val)); // instantiates object of the appropriate type
                }
            }

            // it is empty when none of the query parameters specified matches a column in the DB
            if (!fieldsToVal.isEmpty()) {
                final PrimaryKey primaryKey = new PrimaryKey(fieldsToVal);
                storableKey = new StorableKey(namespace, primaryKey);
            }

            log.debug("Building StorableKey from QueryParam: \n\tnamespace = [{}]\n\t queryParams = [{}]\n\t StorableKey = [{}]",
                    namespace, queryParams, storableKey);
        } catch (Exception e) {
            log.debug("Exception occurred when attempting to generate StorableKey from QueryParam", e);
            throw new IllegalQueryParameterException(e);
        } finally {
            queryExecutor.closeConnection(connection);
        }

        return storableKey;
    }
}
