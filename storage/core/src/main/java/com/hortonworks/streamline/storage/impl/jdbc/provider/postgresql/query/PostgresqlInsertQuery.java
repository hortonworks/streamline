package com.hortonworks.streamline.storage.impl.jdbc.provider.postgresql.query;

import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.impl.jdbc.provider.sql.query.AbstractStorableSqlQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PostgresqlInsertQuery extends AbstractStorableSqlQuery {

    public PostgresqlInsertQuery(Storable storable) {
        super(storable);
    }

    protected Collection<String> getColumnNames(Collection<Schema.Field> columns, final String formatter) {
        Collection<String> collection = new ArrayList<>();
        for (Schema.Field field: columns) {
            if (!field.getName().equalsIgnoreCase("id") || getStorableId() != null) {
                String fieldName = formatter == null ? field.getName() : String.format(formatter, field.getName());
                collection.add(fieldName);
            }
        }
        return collection;
    }

    @Override
    protected String createParameterizedSql() {
        Collection<String> columnNames = getColumnNames(columns, "\"%s\"");;
        String sql = "INSERT INTO \"" + tableName + "\" ("
                + join(columnNames, ", ")
                + ") VALUES( " + getBindVariables("?,", columnNames.size()) + ")";
        LOG.debug(sql);
        return sql;
    }

    @Override
    public List<Schema.Field> getColumns() {
        List<Schema.Field> cols = super.getColumns();
        if (getStorableId() == null) {
            return cols.stream()
                    .filter(f -> !f.getName().equalsIgnoreCase("id"))
                    .collect(Collectors.toList());
        }
        return cols;
    }

    private Long getStorableId() {
        try {
            return getStorable().getId();
        } catch (UnsupportedOperationException ex) {
            // ignore
        }
        return null;
    }
}
