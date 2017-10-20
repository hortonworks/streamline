package com.hortonworks.streamline.storage.impl.jdbc.provider.mysql.query;

import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.AbstractStorableKeyQuery;

public class MysqlSelectQuery extends AbstractStorableKeyQuery {
    public MysqlSelectQuery(String nameSpace) {
        super(nameSpace);
    }

    public MysqlSelectQuery(StorableKey storableKey) {
        super(storableKey);
    }

    @Override
    protected void setParameterizedSql() {
        sql = "SELECT * FROM " + tableName;
        //where clause is defined by columns specified in the PrimaryKey
        if (columns != null) {
            sql += " WHERE " + join(getColumnNames(columns, "`%s` = ?"), " AND ");
        }
        LOG.debug(sql);
    }
}
