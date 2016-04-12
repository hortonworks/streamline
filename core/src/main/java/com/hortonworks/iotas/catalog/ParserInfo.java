package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.PrimaryKey;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.StorableKey;

import java.util.HashMap;
import java.util.Map;

/**
 * Parser information that will be stored by storage layer.
 */
public class ParserInfo extends AbstractStorable {
    public static final String NAME_SPACE = "parser_info";
    public static final String PARSER_ID = "id";
    public static final String PARSER_NAME = "name";
    public static final String CLASS_NAME = "className";
    public static final String JAR_STORAGE_PATH = "jarStoragePath";
    public static final String SCHEMA = "parserSchema";
    public static final String VERSION = "version";
    public static final String TIMESTAMP = "timestamp";

    /**
     * Unique Id for a parser info instance. This is the primary key column.
     */
    private Long id;
    /**
     * Human readable name.
     * (parser name, version) pair is unique constraint.
     */
    private String name;

    /**
     * The parser fully qualified class name that implements the {@code Parser} interface.
     */
    private String className;

    /**
     * Storage location of the jar that contains the Parser implementation.
     */
    private String jarStoragePath;

    /**
     * What schema will {@code Parser} be returned by parser's parse method.
     */
    private Schema parserSchema;

    /**
     * Parser version.
     * NOTE: (parser name, version) pair is unique constraint.
     */
    private Long version;

    /**
     * Time at which this parser was created/updated. //TODO may be we need create and update timestamps.
     */
    private Long timestamp;

    @JsonIgnore
    public String getNameSpace() {
        return NAME_SPACE;
    }

    @JsonIgnore
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldObjectMap = new HashMap<Schema.Field, Object>();
        fieldObjectMap.put(new Schema.Field(PARSER_ID, Schema.Type.LONG), this.id);
        return new PrimaryKey(fieldObjectMap);
    }

    @JsonIgnore
    public StorableKey getStorableKey() {
        return new StorableKey(getNameSpace(), getPrimaryKey());
    }

    @JsonIgnore
    public Schema getSchema() {
        return Schema.of(
                new Schema.Field(PARSER_ID, Schema.Type.LONG),
                new Schema.Field(PARSER_NAME, Schema.Type.STRING),
                new Schema.Field(CLASS_NAME, Schema.Type.STRING),
                new Schema.Field(JAR_STORAGE_PATH, Schema.Type.STRING),
                new Schema.Field(SCHEMA, Schema.Type.STRING),
                new Schema.Field(VERSION, Schema.Type.LONG),
                new Schema.Field(TIMESTAMP, Schema.Type.LONG)
        );
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put(SCHEMA, this.parserSchema.toString()); //TODO this needs to be toJson
        return map;
    }

    public Storable fromMap(Map<String, Object> map) {
        this.parserSchema = Schema.fromString((String) map.remove(SCHEMA));
        super.fromMap(map);
        return this;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getJarStoragePath() {
        return jarStoragePath;
    }

    public void setJarStoragePath(String jarStoragePath) {
        this.jarStoragePath = jarStoragePath;
    }

    @Override
    public String toString() {
        return "ParserInfo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", className='" + className + '\'' +
                ", jarStoragePath='" + jarStoragePath + '\'' +
                ", parserSchema=" + parserSchema +
                ", version=" + version +
                ", timestamp=" + timestamp +
                '}';
    }

    public Schema getParserSchema() {
        return this.parserSchema;
    }

    public void setParserSchema(Schema parserSchema) {
        this.parserSchema = parserSchema;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

        ParserInfo that = (ParserInfo) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (className != null ? !className.equals(that.className) : that.className != null) return false;
        if (jarStoragePath != null ? !jarStoragePath.equals(that.jarStoragePath) : that.jarStoragePath != null)
            return false;
        if (parserSchema != null ? !parserSchema.equals(that.parserSchema) : that.parserSchema != null) return false;
        return !(version != null ? !version.equals(that.version) : that.version != null);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (className != null ? className.hashCode() : 0);
        result = 31 * result + (jarStoragePath != null ? jarStoragePath.hashCode() : 0);
        result = 31 * result + (parserSchema != null ? parserSchema.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }
}
