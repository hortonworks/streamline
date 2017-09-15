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
package com.hortonworks.streamline.storage.impl.jdbc.provider.postgresql.query;

import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.AbstractStorableUpdateQuery;

public class PostgresqlUpdateQuery extends AbstractStorableUpdateQuery {


    public PostgresqlUpdateQuery(Storable storable) {
        super(storable);
    }

    @Override
    protected String createParameterizedSql() {
        String sql = "UPDATE " + tableName + " SET "
                + join(getColumnNames(columns, "\"%s\" = ?"), ", ")
                + " WHERE " + join(getColumnNames(whereFields, "\"%s\" = ?"), " AND ");
        LOG.debug("Sql '{}'", sql);
        return sql;
    }
}
