package com.hortonworks.streamline.storage.impl.jdbc.provider.mysql.query;

import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.AbstractStorableSqlQuery;

public class MysqlInsertQuery extends AbstractStorableSqlQuery {

    public MysqlInsertQuery(Storable storable) {
        super(storable);
    }

    @Override
    protected String createParameterizedSql() {
        String sql = "INSERT INTO " + tableName + " ("
                + join(getColumnNames(columns, "`%s`"), ", ")
                + ") VALUES( " + getBindVariables("?,", columns.size()) + ")";
        LOG.debug(sql);
        return sql;
    }
}
