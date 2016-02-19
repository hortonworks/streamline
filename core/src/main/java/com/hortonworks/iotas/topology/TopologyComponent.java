package com.hortonworks.iotas.topology;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;

import java.util.HashMap;
import java.util.Map;

public class TopologyComponent implements Storable {

    public static final String NAME_SPACE = "topology_component";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String TIMESTAMP = "timestamp";
    public static final String STREAMING_ENGINE = "streamingEngine";
    public static final String SUB_TYPE = "subType";
    public static final String CONFIG = "config";
    public static final String SCHEMA_CLASS = "schemaClass";
    public static final String TRANSFORMATION_CLASS = "transformationClass";

    public enum TopologyComponentType {
        SOURCE,
        PROCESSOR,
        LINK,
        SINK
    }

    /**
     * Unique id for a data stream component. This is the primary key
     */
    private Long id;

    /**
     * User assigned human readable name
     */
    private String name;

    /**
     * Type of the component. For e.g. a SOURCE
     */
    private TopologyComponentType type;

    /**
     * Time recording the creation or last update of this instance
     */
    private Long timestamp;

    /**
     * Underlying streaming engine. For e.g. STORM. This is not an enum
     * because we want the user to be able to add new components without
     * changing code
     */
    private String streamingEngine;

    /**
     * Subtype for this component. For e.g. KAFKA for a source/sink, HBASE
     * for a sink, PARSER, RULE for PROCESSOR, etc. * This is not an enum as
     * we want the user to be able to add new components without changing code
     */
    private String subType;

    /**
     * Json string representing the list of configuration fields for this
     * component
     */
    private String config;

    /**
     * A fully qualified class name that can simulate evolution of schema
     */
    private String schemaClass;

    /**
     * A fully qualified class name that can handle transformation of
     * this component to underlying streaming engine equivalent
     */
    private String transformationClass;

    @Override
    @JsonIgnore
    public String getNameSpace () {
        return NAME_SPACE;
    }

    @Override
    @JsonIgnore
    public Schema getSchema () {
        return Schema.of(
                new Schema.Field(ID, Schema.Type.LONG),
                new Schema.Field(NAME, Schema.Type.STRING),
                new Schema.Field(TYPE, Schema.Type.STRING),
                new Schema.Field(TIMESTAMP, Schema.Type.LONG),
                new Schema.Field(STREAMING_ENGINE, Schema.Type.STRING),
                new Schema.Field(SUB_TYPE, Schema.Type.STRING),
                new Schema.Field(CONFIG, Schema.Type.STRING),
                Schema.Field.optional(SCHEMA_CLASS, Schema.Type.STRING),
                new Schema.Field(TRANSFORMATION_CLASS, Schema.Type.STRING)
        );
    }

    @Override
    @JsonIgnore
    public PrimaryKey getPrimaryKey () {
        Map<Schema.Field, Object> fieldToObjectMap = new HashMap<Schema.Field, Object>();
        fieldToObjectMap.put(new Schema.Field(ID, Schema.Type.LONG),
                this.id);
        return new PrimaryKey(fieldToObjectMap);
    }

    @Override
    @JsonIgnore
    public StorableKey getStorableKey () {
        return new StorableKey(getNameSpace(), getPrimaryKey());
    }

    @Override
    public Map toMap () {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ID, id);
        map.put(NAME, name);
        map.put(TYPE, type.name());
        map.put(TIMESTAMP, timestamp);
        map.put(STREAMING_ENGINE, streamingEngine);
        map.put(SUB_TYPE, subType);
        map.put(CONFIG, config);
        map.put(SCHEMA_CLASS, schemaClass);
        map.put(TRANSFORMATION_CLASS, transformationClass);
        return map;
    }

    @Override
    public Storable fromMap (Map<String, Object> map) {
        id = (Long) map.get(ID);
        name = (String)  map.get(NAME);
        type = TopologyComponentType.valueOf((String) map.get(TYPE));
        timestamp = (Long) map.get(TIMESTAMP);
        streamingEngine = (String) map.get(STREAMING_ENGINE);
        subType = (String) map.get(SUB_TYPE);
        config = (String) map.get(CONFIG);
        schemaClass = (String) map.get(SCHEMA_CLASS);
        transformationClass = (String) map.get(TRANSFORMATION_CLASS);
        return this;
    }

    public Long getId () {
        return id;
    }

    public void setId (Long id) {
        this.id = id;
    }

    public String getName () {
        return name;
    }

    public void setName (String name) {
        this.name = name;
    }

    public TopologyComponentType getType () {
        return type;
    }

    public void setType (TopologyComponentType type) {
        this.type = type;
    }

    public Long getTimestamp () {
        return timestamp;
    }

    public void setTimestamp (Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStreamingEngine () {
        return streamingEngine;
    }

    public void setStreamingEngine (String streamingEngine) {
        this.streamingEngine = streamingEngine;
    }

    public String getSubType () {
        return subType;
    }

    public void setSubType (String subType) {
        this.subType = subType;
    }

    public String getConfig () {
        return config;
    }

    public void setConfig (String config) {
        this.config = config;
    }

    public String getSchemaClass() {
        return schemaClass;
    }

    public void setSchemaClass(String schemaClass) {
        this.schemaClass = schemaClass;
    }

    public String getTransformationClass () {
        return transformationClass;
    }

    public void setTransformationClass (String transformationClass) {
        this.transformationClass = transformationClass;
    }


    @Override
    public String toString () {
        return "TopologyComponent{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", timestamp=" + timestamp +
                ", streamingEngine='" + streamingEngine + '\'' +
                ", subType='" + subType + '\'' +
                ", config='" + config + '\'' +
                ", schemaClass=" + schemaClass + '\'' +
                ", transformationClass='" + transformationClass + '\'' +
                '}';
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TopologyComponent that = (TopologyComponent) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;
        if (type != that.type) return false;
        if (streamingEngine != null ? !streamingEngine.equals(that.streamingEngine) : that.streamingEngine != null)
            return false;
        if (subType != null ? !subType.equals(that.subType) : that.subType != null)
            return false;
        if (config != null ? !config.equals(that.config) : that.config != null)
            return false;
        if (schemaClass != null ? !schemaClass.equals(that.schemaClass) : that.schemaClass != null)
            return false;
        return !(transformationClass != null ? !transformationClass.equals(that.transformationClass) : that.transformationClass != null);

    }

    @Override
    public int hashCode () {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (streamingEngine != null ? streamingEngine.hashCode() : 0);
        result = 31 * result + (subType != null ? subType.hashCode() : 0);
        result = 31 * result + (config != null ? config.hashCode() : 0);
        result = 31 * result + (schemaClass != null ? schemaClass.hashCode() : 0);
        result = 31 * result + (transformationClass != null ? transformationClass.hashCode() : 0);
        return result;
    }



}

