package com.hortonworks.streamline.storage.impl.jdbc.provider.phoenix.query;

import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.AbstractStorableSqlQuery;

/**
 *
 */
public class PhoenixUpsertQuery extends AbstractStorableSqlQuery {

    public PhoenixUpsertQuery(Storable storable) {
        super(storable);
    }

    @Override
    protected void setParameterizedSql() {
        sql = "UPSERT INTO " + tableName + " ("
                + join(getColumnNames(columns, "\"%s\""), ", ")
                + ") VALUES( " + getBindVariables("?,", columns.size()) + ")";
        log.debug(sql);
    }
}
