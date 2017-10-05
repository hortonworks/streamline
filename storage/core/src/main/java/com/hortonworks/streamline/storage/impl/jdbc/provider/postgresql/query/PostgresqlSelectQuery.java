package com.hortonworks.streamline.storage.impl.jdbc.provider.postgresql.query;

import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.AbstractStorableKeyQuery;

public class PostgresqlSelectQuery extends AbstractStorableKeyQuery {
    public PostgresqlSelectQuery(String nameSpace) {
        super(nameSpace);
    }

    public PostgresqlSelectQuery(StorableKey storableKey) {
        super(storableKey);
    }

    @Override
    protected String createParameterizedSql() {
        String sql = "SELECT * FROM \"" + tableName + "\"";
        if (columns != null) {
            sql += " WHERE " + join(getColumnNames(columns, "\"%s\" = ?"), " AND ");
        }
        LOG.debug(sql);
        return sql;
    }
}
