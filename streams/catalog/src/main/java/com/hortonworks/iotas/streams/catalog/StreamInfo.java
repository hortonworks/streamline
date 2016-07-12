/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.streams.catalog;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hortonworks.iotas.common.Schema.Field;
import com.hortonworks.iotas.storage.catalog.AbstractStorable;

/**
 * Catalog db entity for mapping output stream information
 */
public class StreamInfo extends AbstractStorable {
    public static final String NAMESPACE = "streaminfo";
    public static final String ID = "id";
    public static final String STREAMID = "streamId";
    public static final String FIELDSDATA = "fieldsData";
    public static final String TIMESTAMP = "timestamp";
    public static final String FIELDS = "fields";
    public static final String TOPOLOGYID = "topologyId";

    // unique storage level id
    private Long id;

    // the stream identifier string
    private String streamId;

    // the topology that this stream belongs to
    private Long topologyId;

    // list of fields in the stream
    private List<Field> fields;

    // db insert/update timestamp
    private Long timestamp;

    @JsonIgnore
    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    @JsonIgnore
    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field("id", Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @JsonIgnore
    @Override
    public Schema getSchema() {
        return Schema.of(
                Field.of(ID, Schema.Type.LONG),
                Field.of(STREAMID, Schema.Type.STRING),
                Field.of(FIELDSDATA, Schema.Type.STRING), // fields are serialized into fieldsdata
                Field.of(TIMESTAMP, Schema.Type.LONG),
                Field.of(TOPOLOGYID, Schema.Type.LONG)
        );
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    // for internal storage, not part of JSON
    @JsonIgnore
    public String getFieldsData() throws Exception {
        if (fields != null) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(fields);
        }
        return "";
    }

    // for internal storage, not part of JSON
    @JsonIgnore
    public void setFieldsData(String fieldsData) throws Exception {
        if(fieldsData == null || fieldsData.isEmpty()) {
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        fields = mapper.readValue(fieldsData, new TypeReference<List<Field>>() {
        });
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getTopologyId() {
        return topologyId;
    }

    public void setTopologyId(Long topologyId) {
        this.topologyId = topologyId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StreamInfo that = (StreamInfo) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (streamId != null ? !streamId.equals(that.streamId) : that.streamId != null) return false;
        if (topologyId != null ? !topologyId.equals(that.topologyId) : that.topologyId != null) return false;
        if (fields != null ? !fields.equals(that.fields) : that.fields != null) return false;
        return timestamp != null ? timestamp.equals(that.timestamp) : that.timestamp == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (streamId != null ? streamId.hashCode() : 0);
        result = 31 * result + (topologyId != null ? topologyId.hashCode() : 0);
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StreamInfo{" +
                "id=" + id +
                ", streamId='" + streamId + '\'' +
                ", topologyId=" + topologyId +
                ", fields=" + fields +
                ", timestamp=" + timestamp +
                "}";
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.remove(FIELDS);
        try {
            map.put(FIELDSDATA, getFieldsData());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return map;
    }
}
