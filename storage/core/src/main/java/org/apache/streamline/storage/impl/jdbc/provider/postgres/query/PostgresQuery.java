package org.apache.streamline.storage.impl.jdbc.provider.postgres.query;

import org.apache.streamline.storage.impl.jdbc.provider.sql.query.AbstractSqlQuery;

public class PostgresQuery extends AbstractSqlQuery {

    public PostgresQuery(String sql) {
        this.sql = sql;
    }

    @Override
    protected void setParameterizedSql() {
        log.debug(sql);
    }
}

