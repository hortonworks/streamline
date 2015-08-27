package com.hortonworks.hbase;

import backtype.storm.tuple.Tuple;
import com.google.common.base.Charsets;
import com.hortonworks.bolt.ParserBolt;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hbase.common.ColumnList;

import java.util.Map;

import static org.apache.storm.hbase.common.Utils.toBytes;

public class ParserOutputHBaseMapper implements HBaseMapper {
    private String rowKeyField;
    private byte[] columnFamily;

    //TODO need to support counter fields

    public ParserOutputHBaseMapper(String rowKeyField, String columnFamily) {
        this.rowKeyField = rowKeyField;
        this.columnFamily = columnFamily.getBytes(Charsets.UTF_8);
    }

    @Override
    public byte[] rowKey(Tuple tuple) {
        Map<String, Object> parsedMap = (Map<String, Object>) tuple.getValueByField(ParserBolt.PARSED_FIELDS);
        return toBytes(parsedMap.get(rowKeyField));
    }

    @Override
    public ColumnList columns(Tuple tuple) {
        Map<String, Object> parsedMap = (Map<String, Object>) tuple.getValueByField(ParserBolt.PARSED_FIELDS);
        ColumnList columnList = new ColumnList();
        for (String key : parsedMap.keySet()) {
            //Hbase bolt can not handle null values.
            if (!key.equals(rowKeyField) && parsedMap.get(key) != null) {
                columnList.addColumn(columnFamily, key.getBytes(Charsets.UTF_8), toBytes(parsedMap.get(key)));
            }
        }
        return columnList;
    }
}
