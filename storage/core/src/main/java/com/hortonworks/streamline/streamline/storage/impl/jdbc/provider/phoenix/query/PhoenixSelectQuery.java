package com.hortonworks.streamline.storage.impl.jdbc.provider.phoenix.query;

import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.AbstractStorableKeyQuery;

/**
 *
 */
public class PhoenixSelectQuery extends AbstractStorableKeyQuery {

    public PhoenixSelectQuery(String nameSpace) {
        super(nameSpace);
    }

    public PhoenixSelectQuery(StorableKey storableKey) {
        super(storableKey);
    }

    @Override
    protected void setParameterizedSql() {
        sql = "SELECT * FROM " + tableName;
        //where clause is defined by columns specified in the PrimaryKey
        if (columns != null) {
            sql += " WHERE " + join(getColumnNames(columns, "\"%s\" = ?"), " AND ");
        }
        log.debug(sql);
    }
}
