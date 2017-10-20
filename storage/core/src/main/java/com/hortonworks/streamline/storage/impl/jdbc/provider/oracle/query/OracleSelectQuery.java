/**
 * Copyright 2017 Hortonworks.
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

package com.hortonworks.streamline.storage.impl.jdbc.provider.oracle.query;

import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.impl.jdbc.provider.oracle.exception.OracleQueryException;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.AbstractStorableKeyQuery;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class OracleSelectQuery extends AbstractStorableKeyQuery {

    public OracleSelectQuery(String nameSpace) {
        super(nameSpace);
    }

    public OracleSelectQuery(StorableKey storableKey) {
        super(storableKey);
    }

    @Override
    protected void setParameterizedSql() {
        sql = "SELECT * FROM \"" + tableName + "\"";
        if (columns != null) {
            List<String> whereClauseColumns = new LinkedList<>();
            for (Map.Entry<Schema.Field, Object> columnKeyValue : primaryKey.getFieldsToVal().entrySet()) {
                if (columnKeyValue.getKey().getType() == Schema.Type.STRING) {
                    String stringValue = (String) columnKeyValue.getValue();
                    if ((stringValue).length() > 4000) {
                        throw new OracleQueryException(String.format("Column \"%s\" of the table \"%s\" is compared against a value \"%s\", " +
                                        "which is greater than 4k characters",
                                columnKeyValue.getKey().getName(), tableName, stringValue));
                    } else
                        whereClauseColumns.add(String.format(" to_char(\"%s\") = ?", columnKeyValue.getKey().getName()));
                } else {
                    whereClauseColumns.add(String.format(" \"%s\" = ?", columnKeyValue.getKey().getName()));
                }
            }
            sql += " WHERE " + join(whereClauseColumns, " AND ");
        }
        LOG.debug(sql);
    }
}
