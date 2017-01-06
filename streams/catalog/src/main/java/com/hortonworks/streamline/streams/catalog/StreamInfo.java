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
package com.hortonworks.streamline.streams.catalog;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import com.hortonworks.streamline.common.Schema;
import com.hortonworks.streamline.common.Schema.Field;
import com.hortonworks.streamline.storage.PrimaryKey;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.catalog.AbstractStorable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Catalog db entity for mapping output stream information
 */
public class StreamInfo extends AbstractStorable {
    public static final String NAMESPACE = "streaminfo";
    public static final String ID = "id";
    public static final String VERSIONID = "versionId";
    public static final String STREAMID = "streamId";
    public static final String DESCRIPTION = "description";
    public static final String FIELDSDATA = "fieldsData";
    public static final String TIMESTAMP = "timestamp";
    public static final String FIELDS = "fields";
    public static final String TOPOLOGYID = "topologyId";

    // unique storage level id
    private Long id;

    private Long versionId;

    // the stream identifier string
    private String streamId;

    // description
    private String description;

    // the topology that this stream belongs to
    private Long topologyId;

    // list of fields in the stream
    private List<Field> fields;

    private Long versionTimestamp;

    public StreamInfo() {
    }

    // copy ctor
    public StreamInfo(StreamInfo other) {
        setId(other.getId());
        setVersionId(other.getVersionId());
        setStreamId(other.getStreamId());
        setDescription(other.getDescription());
        setTopologyId(other.getTopologyId());
        if (other.getFields() != null) {
            setFields(other.getFields().stream().map(Field::copy).collect(Collectors.toList()));
        }
        setVersionTimestamp(other.getVersionTimestamp());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("timestamp")
    public Long getVersionTimestamp() {
        return versionTimestamp;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("timestamp")
    public void setVersionTimestamp(Long timestamp) {
        this.versionTimestamp = timestamp;
    }

    @JsonIgnore
    @Override
    public String getNameSpace() {
        return NAMESPACE;
    }

    @JsonIgnore
    @Override
    public PrimaryKey getPrimaryKey() {
        Map<Field, Object> fieldToObjectMap = new HashMap<>();
        fieldToObjectMap.put(new Schema.Field(ID, Schema.Type.LONG), this.id);
        fieldToObjectMap.put(new Schema.Field(VERSIONID, Schema.Type.LONG), this.versionId);
        return new PrimaryKey(fieldToObjectMap);
    }

    @JsonIgnore
    @Override
    public Schema getSchema() {
        return Schema.of(
                Field.of(ID, Schema.Type.LONG),
                Field.of(VERSIONID, Schema.Type.LONG),
                Field.of(STREAMID, Schema.Type.STRING),
                Field.of(DESCRIPTION, Schema.Type.STRING),
                Field.of(FIELDSDATA, Schema.Type.STRING), // fields are serialized into fieldsdata
                Field.of(TOPOLOGYID, Schema.Type.LONG)
        );
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
            return mapper.writerFor(new TypeReference<List<Field>>() {
            }).writeValueAsString(fields);
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
        return versionId != null ? versionId.equals(that.versionId) : that.versionId == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (versionId != null ? versionId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StreamInfo{" +
                "id=" + id +
                ", versionId=" + versionId +
                ", streamId='" + streamId + '\'' +
                ", description='" + description + '\'' +
                ", topologyId=" + topologyId +
                ", fields=" + fields +
                ", versionTimestamp=" + versionTimestamp +
                "} " + super.toString();
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

    @Override
    public Storable fromMap(Map<String, Object> map) {
        setId((Long) map.get(ID));
        setVersionId((Long) map.get(VERSIONID));
        setStreamId((String) map.get(STREAMID));
        setDescription((String) map.get(DESCRIPTION));
        setTopologyId((Long) map.get(TOPOLOGYID));
        ObjectMapper mapper = new ObjectMapper();
        String fieldsDataStr = (String) map.get(FIELDSDATA);
        if (!StringUtils.isEmpty(fieldsDataStr)) {
            List<Field> fields;
            try {
                fields = mapper.readValue(fieldsDataStr, new TypeReference<List<Field>>() {});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            setFields(fields);
        }
        return this;
    }
}
