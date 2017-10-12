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


package com.hortonworks.streamline.streams.notification.store.hbase.mappers;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A mapper for the StreamlineEvent
 */
public class StreamlineEventMapper implements Mapper<StreamlineEvent> {

    // TODO: the table should be changed to "StreamlineEvent"
    private static final String TABLE_NAME = "nest";

    private static final byte[] CF_FIELDS = "cf".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CF_DATASOURCE_ID = "d".getBytes(StandardCharsets.UTF_8);

    @Override
    public List<TableMutation> tableMutations(StreamlineEvent event) {
        throw new UnsupportedOperationException("Not implemented, StreamlineEvent are currently inserted via HbaseBolt.");
    }

    @Override
    public StreamlineEvent entity(Result result) {
        String id = Bytes.toString(result.getRow());
        Map<String, Object> fieldsAndValues = new HashMap<>();
        for(Map.Entry<byte[], byte[]> entry: result.getFamilyMap(CF_FIELDS).entrySet()) {
            fieldsAndValues.put(Bytes.toString(entry.getKey()), Bytes.toString(entry.getValue()));
        }
        String dataSourceId = Bytes.toString(result.getFamilyMap(CF_DATASOURCE_ID).firstEntry().getKey());
        StreamlineEventImpl event = StreamlineEventImpl.builder().fieldsAndValues(fieldsAndValues).dataSourceId(dataSourceId).build();
        return new StreamlineEventWithID(event, id);
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public List<byte[]> mapMemberValue(String memberName, String value) {
        // Does not support querying by field.
        return null;
    }

    // keep the implementation in this class unless there're other cases which needs to associate ID as well
    private static class StreamlineEventWithID implements StreamlineEvent {

        private StreamlineEvent underlyingEvent;
        private String id;

        public StreamlineEventWithID(StreamlineEvent underlyingEvent, String id) {
            this.underlyingEvent = underlyingEvent;
            this.id = id;
        }

        @Override
        public Map<String, Object> getAuxiliaryFieldsAndValues() {
            return underlyingEvent.getAuxiliaryFieldsAndValues();
        }

        @Override
        public StreamlineEvent addAuxiliaryFieldAndValue(String field, Object value) {
            // note that returning event doesn't preserve ID
            return underlyingEvent.addAuxiliaryFieldAndValue(field, value);
        }

        @Override
        public Map<String, Object> getHeader() {
            return underlyingEvent.getHeader();
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getDataSourceId() {
            return underlyingEvent.getDataSourceId();
        }

        @Override
        public String getSourceStream() {
            return underlyingEvent.getSourceStream();
        }

        @Override
        public StreamlineEvent addFieldsAndValues(Map<String, Object> fieldsAndValues) {
            return underlyingEvent.addFieldsAndValues(fieldsAndValues);
        }

        @Override
        public StreamlineEvent addFieldAndValue(String key, Object value) {
            return underlyingEvent.addFieldAndValue(key, value);
        }

        @Override
        public StreamlineEvent addHeaders(Map<String, Object> headers) {
            return underlyingEvent.addHeaders(headers);
        }

        @Override
        public byte[] getBytes() {
            try {
                return this.toString().getBytes("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public int size() {
            return underlyingEvent.size();
        }

        @Override
        public boolean isEmpty() {
            return underlyingEvent.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return underlyingEvent.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return underlyingEvent.containsValue(value);
        }

        @Override
        public Object get(Object key) {
            return underlyingEvent.get(key);
        }

        @Override
        public Set<String> keySet() {
            return underlyingEvent.keySet();
        }

        @Override
        public Collection<Object> values() {
            return underlyingEvent.values();
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return underlyingEvent.entrySet();
        }

        @Override
        public String toString() {
            return "StreamlineEventWithID{" +
                    "underlyingEvent=" + underlyingEvent +
                    ", id='" + id + '\'' +
                    '}';
        }
    }
}
