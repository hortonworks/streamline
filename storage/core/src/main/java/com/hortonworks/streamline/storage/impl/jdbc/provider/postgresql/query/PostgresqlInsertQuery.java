package com.hortonworks.streamline.storage.impl.jdbc.provider.postgresql.query;

import com.hortonworks.streamline.common.Schema;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.AbstractStorableSqlQuery;

import java.util.ArrayList;
import java.util.Collection;

public class PostgresqlInsertQuery extends AbstractStorableSqlQuery {

    public PostgresqlInsertQuery(Storable storable) {
        super(storable);
    }

    protected Collection<String> getColumnNames(Collection<Schema.Field> columns, final String formatter) {
        Collection<String> collection = new ArrayList<>();
        for (Schema.Field field: columns) {
            if (!field.getName().equalsIgnoreCase("id")) {
                String fieldName = formatter == null ? field.getName() : String.format(formatter, field.getName());
                collection.add(fieldName);
            }
        }
        return collection;
    }

    @Override
    protected void setParameterizedSql() {
        Collection<String> columnNames = getColumnNames(columns, "\"%s\"");;
        sql = "INSERT INTO " + tableName + " ("
                + join(columnNames, ", ")
                + ") VALUES( " + getBindVariables("?,", columnNames.size()) + ")";
        log.debug(sql);
    }
}
