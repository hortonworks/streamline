package org.apache.streamline.storage.impl.jdbc.provider.postgresql.query;

import org.apache.streamline.storage.impl.jdbc.provider.sql.query.AbstractSqlQuery;

public class PostgresqlQuery extends AbstractSqlQuery {

    public PostgresqlQuery(String sql) {
        this.sql = sql;
    }

    @Override
    protected void setParameterizedSql() {
        log.debug(sql);
    }
}

