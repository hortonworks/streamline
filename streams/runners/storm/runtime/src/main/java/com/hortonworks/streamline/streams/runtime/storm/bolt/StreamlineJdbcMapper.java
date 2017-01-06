package com.hortonworks.streamline.streams.runtime.storm.bolt;

import org.apache.storm.jdbc.common.Column;
import org.apache.storm.jdbc.common.ConnectionProvider;
import org.apache.storm.jdbc.common.JdbcClient;
import org.apache.storm.jdbc.common.Util;
import org.apache.storm.jdbc.mapper.JdbcMapper;
import org.apache.storm.tuple.ITuple;
import com.hortonworks.streamline.streams.StreamlineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StreamlineJdbcMapper implements JdbcMapper {
    private static final Logger LOG = LoggerFactory.getLogger(StreamlineJdbcMapper.class);

    private final String tableName;
    private final ConnectionProvider connectionProvider;
    private final List<String> fields;
    private final Map<String, Column> fieldsToColumns = new HashMap<>();

    public StreamlineJdbcMapper(String tableName,
                                ConnectionProvider connectionProvider,
                                List<String> fields) {
        this.tableName = tableName;
        this.connectionProvider = connectionProvider;
        this.fields = fields;
        LOG.info("JDBC Mapper initialized with tableName: {}, connectionProvider: {}, fields: {}",
                tableName, connectionProvider, fields);
    }

    @Override
    public List<Column> getColumns(ITuple tuple) {
        StreamlineEvent event = (StreamlineEvent) tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
        List<Column> res = new ArrayList<>();
        if (fieldsToColumns == null) {
            initFieldsToColumns();
        }
        for(String field: fields) {
            Column<?> column = getColumn(field);
            String columnName = column.getColumnName();
            Integer columnSqlType = column.getSqlType();
            Object value = Util.getJavaType(columnSqlType).cast(event.get(field));
            res.add(new Column<>(columnName, value, columnSqlType));
        }
        return res;
    }

    private void initFieldsToColumns() {
        connectionProvider.prepare();
        JdbcClient client = new JdbcClient(connectionProvider, 30);
        for (Column<?> column: client.getColumnSchema(tableName)) {
            fieldsToColumns.put(column.getColumnName().toUpperCase(), column);
        }
    }

    private Column<?> getColumn(String fieldName) {
        Column<?> column = fieldsToColumns.get(fieldName.toUpperCase());
        if (column != null) {
            return column;
        }
        throw new IllegalArgumentException("Could not find database column: " + fieldName);
    }
}
