package com.hortonworks.iotas.catalog;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hortonworks.iotas.common.Schema.Field;

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

    // unique storage level id
    private Long id;

    // the stream identifier string
    private String streamId;

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
                Field.of(TIMESTAMP, Schema.Type.LONG)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StreamInfo that = (StreamInfo) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (streamId != null ? !streamId.equals(that.streamId) : that.streamId != null) return false;
        if (fields != null ? !fields.equals(that.fields) : that.fields != null) return false;
        return timestamp != null ? timestamp.equals(that.timestamp) : that.timestamp == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (streamId != null ? streamId.hashCode() : 0);
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StreamInfo{" +
                "id=" + id +
                ", streamId='" + streamId + '\'' +
                ", fields=" + fields +
                ", timestamp=" + timestamp +
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
}
