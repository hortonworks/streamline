package org.apache.streamline.storage.impl.jdbc.provider.phoenix.query;

import org.apache.streamline.storage.StorableKey;
import org.apache.streamline.storage.impl.jdbc.provider.sql.query.AbstractStorableKeyQuery;

/**
 *
 */
public class PhoenixDeleteQuery extends AbstractStorableKeyQuery {

    public PhoenixDeleteQuery(String nameSpace) {
        super(nameSpace);
    }

    public PhoenixDeleteQuery(StorableKey storableKey) {
        super(storableKey);
    }

    @Override
    protected void setParameterizedSql() {
        sql = "DELETE FROM  " + tableName + " WHERE " + join(getColumnNames(columns, "\"%s\" = ?"), " AND ");
        log.debug(sql);
    }
}
