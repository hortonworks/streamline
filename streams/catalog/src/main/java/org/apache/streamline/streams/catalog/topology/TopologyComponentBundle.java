package org.apache.streamline.streams.catalog.topology;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.common.Schema;
import org.apache.streamline.storage.PrimaryKey;
import org.apache.streamline.storage.Storable;
import org.apache.streamline.storage.StorableKey;
import org.apache.streamline.streams.layout.storm.FluxComponent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TopologyComponentBundle implements Storable {

    public static final String NAME_SPACE = "topology_component_bundles";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String TIMESTAMP = "timestamp";
    public static final String STREAMING_ENGINE = "streamingEngine";
    public static final String SUB_TYPE = "subType";
    public static final String UI_SPECIFICATION = "topologyComponentUISpecification";
    public static final String BUNDLE_JAR = "bundleJar";
    public static final String SCHEMA_CLASS = "schemaClass";
    public static final String TRANSFORMATION_CLASS = "transformationClass";
    public static final String BUILTIN = "builtin";
    public static final String MAVEN_DEPS = "mavenDeps";

    public enum TopologyComponentType {
        SOURCE,
        PROCESSOR,
        LINK,
        SINK,
        ACTION,
        TRANSFORM,
        TOPOLOGY
    }

    /**
     * Unique id for a topology bundle component. This is the primary key
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
     * Jar filename path for the bundle that is expected to have implementation of
     * {@link FluxComponent} for storm
     */
    private String bundleJar;

    /**
     * Object that will be used by ui to elicit values from user for this component
     * when dropped on to streams builder
     */
    private TopologyComponentUISpecification topologyComponentUISpecification;

    /**
     * A fully qualified class name that can simulate evolution of schema
     */
    private String schemaClass;

    /**
     * A fully qualified class name that can handle transformation of
     * this component to underlying streaming engine equivalent
     */
    private String transformationClass;

    /**
     * Boolean indicating if this bundle is built in and shipped with streams or add on
     * If true, jar file not expected to be uploaded for the bundle
     */
    private Boolean builtin = false;

    /**
     * A comma separated string representing the maven jar dependencies to be pulled while submitting a storm topology
     * that has a component tied to this bundle. Format of this field is as supported by the --artifacts option in storm.py
     */
    private String mavenDeps;

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
                new Schema.Field(BUNDLE_JAR, Schema.Type.STRING),
                new Schema.Field(UI_SPECIFICATION, Schema.Type.STRING),
                Schema.Field.optional(SCHEMA_CLASS, Schema.Type.STRING),
                new Schema.Field(TRANSFORMATION_CLASS, Schema.Type.STRING),
                new Schema.Field(BUILTIN, Schema.Type.STRING),
                new Schema.Field(MAVEN_DEPS, Schema.Type.STRING)
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
        ObjectMapper mapper = new ObjectMapper();
        String uiSpecification;
        try {
            uiSpecification = mapper.writeValueAsString(topologyComponentUISpecification);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ID, id);
        map.put(NAME, name);
        map.put(TYPE, type.name());
        map.put(TIMESTAMP, timestamp);
        map.put(STREAMING_ENGINE, streamingEngine);
        map.put(SUB_TYPE, subType);
        map.put(BUNDLE_JAR, bundleJar);
        map.put(UI_SPECIFICATION, uiSpecification);
        map.put(SCHEMA_CLASS, schemaClass);
        map.put(TRANSFORMATION_CLASS, transformationClass);
        map.put(BUILTIN, builtin.toString());
        map.put(MAVEN_DEPS, mavenDeps);
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
        bundleJar = (String) map.get(BUNDLE_JAR);
        ObjectMapper mapper = new ObjectMapper();
        try {
            topologyComponentUISpecification = mapper.readValue((String) map.get(UI_SPECIFICATION), TopologyComponentUISpecification.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        schemaClass = (String) map.get(SCHEMA_CLASS);
        transformationClass = (String) map.get(TRANSFORMATION_CLASS);
        builtin = Boolean.valueOf((String) map.get(BUILTIN));
        mavenDeps = (String) map.get(MAVEN_DEPS);
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

    public String getBundleJar () {
        return bundleJar;
    }

    public void setBundleJar (String bundleJar) {
        this.bundleJar = bundleJar;
    }

    public TopologyComponentUISpecification getTopologyComponentUISpecification () {
        return topologyComponentUISpecification;
    }

    public void setTopologyComponentUISpecification (TopologyComponentUISpecification topologyComponentUISpecification) {
        this.topologyComponentUISpecification = topologyComponentUISpecification;
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

    public Boolean getBuiltin () {
        return builtin;
    }

    public void setBuiltin (Boolean builtin) {
       this.builtin = builtin;
    }

    public String getMavenDeps() {
        return mavenDeps;
    }

    public void setMavenDeps(String mavenDeps) {
        this.mavenDeps = mavenDeps;
    }

    @Override
    public String toString () {
        return "TopologyComponentBundle{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", timestamp=" + timestamp +
                ", streamingEngine='" + streamingEngine + '\'' +
                ", subType='" + subType + '\'' +
                ", bundleJar='" + bundleJar + '\'' +
                ", topologyComponentUISpecification='" + topologyComponentUISpecification + '\'' +
                ", schemaClass=" + schemaClass + '\'' +
                ", transformationClass='" + transformationClass + '\'' +
                ", builtin='" + builtin + '\'' +
                ", mavenDeps='" + mavenDeps + '\'' +
                '}';
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TopologyComponentBundle that = (TopologyComponentBundle) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;
        if (type != that.type) return false;
        if (streamingEngine != null ? !streamingEngine.equals(that.streamingEngine) : that.streamingEngine != null)
            return false;
        if (subType != null ? !subType.equals(that.subType) : that.subType != null)
            return false;
        if (bundleJar != null ? !bundleJar.equals(that.bundleJar) : that.bundleJar!= null)
            return false;
        if (topologyComponentUISpecification != null ? !topologyComponentUISpecification.equals(that.topologyComponentUISpecification) : that.topologyComponentUISpecification!= null)
            return false;
        if (schemaClass != null ? !schemaClass.equals(that.schemaClass) : that.schemaClass != null)
            return false;
        if (builtin != null ? !builtin.equals(that.builtin) : that.builtin!= null)
            return false;
        if (mavenDeps != null ? !mavenDeps.equals(that.mavenDeps) : that.mavenDeps!= null)
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
        result = 31 * result + (bundleJar != null ? bundleJar.hashCode() : 0);
        result = 31 * result + (topologyComponentUISpecification != null ? topologyComponentUISpecification.hashCode() : 0);
        result = 31 * result + (schemaClass != null ? schemaClass.hashCode() : 0);
        result = 31 * result + (transformationClass != null ? transformationClass.hashCode() : 0);
        result = 31 * result + (builtin != null ? builtin.hashCode() : 0);
        result = 31 * result + (mavenDeps != null ? mavenDeps.hashCode() : 0);
        return result;
    }


}

