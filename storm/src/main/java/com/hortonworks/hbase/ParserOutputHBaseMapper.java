package com.hortonworks.hbase;

import backtype.storm.tuple.Tuple;
import com.google.common.base.Charsets;
import com.hortonworks.bolt.ParserBolt;
import com.hortonworks.iotas.common.IotasEvent;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hbase.common.ColumnList;

import java.util.Map;

import static org.apache.storm.hbase.common.Utils.toBytes;

public class ParserOutputHBaseMapper implements HBaseMapper {
    private byte[] columnFamily;

    //TODO need to support counter fields

    public ParserOutputHBaseMapper(String columnFamily) {
        this.columnFamily = columnFamily.getBytes(Charsets.UTF_8);
    }

    @Override
    public byte[] rowKey(Tuple tuple) {
        IotasEvent event = (IotasEvent) tuple.getValueByField(ParserBolt.IOTAS_EVENT);
        return toBytes(event.getId());
    }

    @Override
    public ColumnList columns(Tuple tuple) {
        IotasEvent event = (IotasEvent) tuple.getValueByField(ParserBolt.IOTAS_EVENT);
        Map<String, Object> parsedMap = event.getFieldsAndValues();

        ColumnList columnList = new ColumnList();
        for (String key : parsedMap.keySet()) {
            //Hbase bolt can not handle null values.
            if (parsedMap.get(key) != null) {
                columnList.addColumn(columnFamily, key.getBytes(Charsets.UTF_8), toBytes(parsedMap.get(key)));
            }
        }
        return columnList;
    }
}
