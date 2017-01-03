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
package org.apache.streamline.storage.impl.jdbc.provider.postgresql.query;


import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.apache.streamline.common.Schema;
import org.apache.streamline.storage.Storable;
import org.apache.streamline.storage.impl.jdbc.provider.sql.query.AbstractStorableSqlQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PostgresqlInsertUpdateDuplicate extends AbstractStorableSqlQuery {

    public PostgresqlInsertUpdateDuplicate(Storable storable) {
        super(storable);
    }

    /*@Override
    /**
     * if formatter != null applies the formatter to the column names. Examples of output are:
     * <p/>
     * formatter == null ==> [colName1, colName2]
     * <p/>
     * formatter == "%s = ?" ==> [colName1 = ?, colName2 = ?]
     */
    protected Collection<String> getColumnNames(Collection<Schema.Field> columns, final String formatter) {
        Collection<String> collection = new ArrayList<>();
        for (Schema.Field field: columns) {
            if (!field.getName().equalsIgnoreCase("id")) {
                String fieldName = formatter == null ? field.getName() : String.format(formatter, field.getName());
                collection.add(fieldName);
            }
        }
        return collection;
    }



    // the factor of 2 comes from the fact that each column is referred twice in the MySql query as follows
    // "INSERT INTO DB.TABLE (id, name, age) VALUES(1, "A", 19) ON DUPLICATE KEY UPDATE id=1, name="A", age=19";
    @Override
    protected void setParameterizedSql() {

        sql = "INSERT INTO " + tableName + " ("
                + join(getColumnNames(columns, "%s"), ", ")
                + ") VALUES(" + getBindVariables("?,", columns.size() - 1) + ")"
                + " ON CONFLICT  DO UPDATE SET " + join(getColumnNames(columns, "%s = ?"), ", ");
        log.debug(sql);
    }
}

