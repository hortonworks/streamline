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
package com.hortonworks.iotas.storage.impl.jdbc.provider.sql.query;

import com.hortonworks.iotas.storage.StorableKey;

public class SqlSelectQuery extends AbstractStorableKeyQuery {
    public SqlSelectQuery(String nameSpace) {
        super(nameSpace);   // super.columns == null => no where clause filtering
    }

    public SqlSelectQuery(StorableKey storableKey) {
        super(storableKey);     // super.columns != null => do where clause filtering on PrimaryKey
    }

    // "SELECT * FROM DB.TABLE [WHERE C1 = ?, C2 = ?]"
    @Override
    protected void setParameterizedSql() {
        sql = "SELECT * FROM " + tableName;
        //where clause is defined by columns specified in the PrimaryKey
        if (columns != null) {
            sql += " WHERE " + join(getColumnNames(columns, "%s = ?"), " AND ");
        }
        log.debug(sql);
    }
}
