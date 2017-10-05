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

import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.AbstractStorableKeyQuery;

public class OracleDeleteQuery extends AbstractStorableKeyQuery {

    public OracleDeleteQuery(String nameSpace) {
        super(nameSpace);
    }

    public OracleDeleteQuery(StorableKey storableKey) {
        super(storableKey);
    }

    @Override
    protected String createParameterizedSql() {
        String sql = "DELETE FROM \"" + tableName + "\" WHERE " + join(getColumnNames(columns, "\"%s\" = ?"), " AND ");
        LOG.debug(sql);
        return sql;
    }
}
