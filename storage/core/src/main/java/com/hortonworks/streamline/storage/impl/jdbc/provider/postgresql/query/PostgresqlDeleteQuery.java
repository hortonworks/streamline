package com.hortonworks.streamline.storage.impl.jdbc.provider.postgresql.query;

import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.AbstractStorableKeyQuery;

public class PostgresqlDeleteQuery extends AbstractStorableKeyQuery {

    public PostgresqlDeleteQuery(String nameSpace) {
        super(nameSpace);
    }

    public PostgresqlDeleteQuery(StorableKey storableKey) {
        super(storableKey);
    }

    @Override
    protected String createParameterizedSql() {
        String sql = "DELETE FROM  \"" + tableName + "\" WHERE " + join(getColumnNames(columns, "\"%s\" = ?"), " AND ");
        LOG.debug(sql);
        return sql;
    }
}
