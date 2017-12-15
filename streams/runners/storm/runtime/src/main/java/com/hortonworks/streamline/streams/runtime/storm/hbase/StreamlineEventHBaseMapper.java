/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.streams.runtime.storm.hbase;

import com.google.common.base.Charsets;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.runtime.storm.StreamlineRuntimeUtil;
import org.apache.storm.hbase.bolt.mapper.HBaseMapper;
import org.apache.storm.hbase.common.ColumnList;
import org.apache.storm.tuple.Tuple;

import static org.apache.storm.hbase.common.Utils.toBytes;

public class StreamlineEventHBaseMapper implements HBaseMapper {
    private final byte[] columnFamily;
    private final String rowKeyField;

    public StreamlineEventHBaseMapper(String columnFamily) {
        this(columnFamily, null);
    }

    public StreamlineEventHBaseMapper(String columnFamily, String rowKeyField) {
        this.columnFamily = columnFamily.getBytes(Charsets.UTF_8);
        this.rowKeyField = rowKeyField;
    }

    @Override
    public byte[] rowKey(Tuple tuple) {
        StreamlineEvent event = (StreamlineEvent) tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
        return toBytes((rowKeyField != null && !rowKeyField.isEmpty()) ? StreamlineRuntimeUtil.getFieldValue(event, rowKeyField) : event.getId());
    }

    @Override
    public ColumnList columns(Tuple tuple) {
        StreamlineEvent event = (StreamlineEvent) tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);

        ColumnList columnList = new ColumnList();
        for (String key : event.keySet()) {
            //Hbase bolt can not handle null values.
            if (event.get(key) != null) {
                columnList.addColumn(columnFamily, key.getBytes(Charsets.UTF_8), toBytes(event.get(key)));
            }
        }
        return columnList;
    }
}
