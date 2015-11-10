package com.hortonworks.hbase;

import backtype.storm.tuple.Tuple;
import com.google.common.base.Charsets;
import com.hortonworks.iotas.common.IotasEvent;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hbase.common.ColumnList;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.apache.storm.hbase.common.Utils.toBytes;

public class ParserOutputHBaseMapper implements HBaseMapper {
    private byte[] columnFamily;
    //TODO need to support counter fields
    private static final byte[] CF_DSRCID = "d".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CV_DEFAULT = "1".getBytes(StandardCharsets.UTF_8);

    public ParserOutputHBaseMapper(String columnFamily) {
        this.columnFamily = columnFamily.getBytes(Charsets.UTF_8);
    }

    @Override
    public byte[] rowKey(Tuple tuple) {
        IotasEvent event = (IotasEvent) tuple.getValueByField(IotasEvent.IOTAS_EVENT);
        return toBytes(event.getId());
    }

    @Override
    public ColumnList columns(Tuple tuple) {
        IotasEvent event = (IotasEvent) tuple.getValueByField(IotasEvent.IOTAS_EVENT);
        Map<String, Object> parsedMap = event.getFieldsAndValues();

        ColumnList columnList = new ColumnList();
        for (String key : parsedMap.keySet()) {
            //Hbase bolt can not handle null values.
            if (parsedMap.get(key) != null) {
                columnList.addColumn(columnFamily, key.getBytes(Charsets.UTF_8), toBytes(parsedMap.get(key)));
            }
        }
        columnList.addColumn(CF_DSRCID, event.getDataSourceId().getBytes(StandardCharsets.UTF_8), CV_DEFAULT);
        return columnList;
    }
}
