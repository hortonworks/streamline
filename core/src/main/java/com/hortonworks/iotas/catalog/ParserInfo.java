package com.hortonworks.iotas.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.PrimaryKey;

import java.util.HashMap;
import java.util.Map;

/**
 * Parser information that will be stored by storage layer.
 */
public class ParserInfo implements Storable {
    public static final String PARSER_ID = "parserId";
    public static final String PARSER_NAME = "parserName";
    public static final String CLASS_NAME = "className";
    public static final String JAR_STORAGE_PATH = "jarStoragePath";
    public static final String SCHEMA = "parserSchema";
    public static final String VERSION = "version";
    public static final String TIMESTAMP = "timestamp";

    /**
     * Unique Id for a parser info instance. This is the primary key column.
     */
    private Long parserId;
    /**
     * Human redabale name.
     */
    private String parserName;

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
     * TODO do we need a version when parserId is uniquly identifying a parser instance?
     * Or should we remove parserId and make ParserName and version as the PK?
     */
    private Long version;

    /**
     * Time at which this parser was created/updated. //TODO may be we need create and update timestamps.
     */
    private Long timestamp;

    @JsonIgnore
    public String getNameSpace() {
        return "parser-info";
    }

    @JsonIgnore
    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldObjectMap = new HashMap<Schema.Field, Object>();
        fieldObjectMap.put(new Schema.Field(PARSER_ID, Schema.Type.LONG), this.parserId);
        return new PrimaryKey(fieldObjectMap);
    }

    @JsonIgnore
    public Schema getSchema() {
        return new Schema(
                new Schema.Field(PARSER_ID, Schema.Type.LONG),
                new Schema.Field(PARSER_NAME, Schema.Type.STRING),
                new Schema.Field(CLASS_NAME, Schema.Type.STRING),
                new Schema.Field(JAR_STORAGE_PATH, Schema.Type.STRING),
                new Schema.Field(SCHEMA, Schema.Type.STRING),
                new Schema.Field(VERSION, Schema.Type.LONG),
                new Schema.Field(TIMESTAMP, Schema.Type.LONG)
        );
    }

    public Map toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(PARSER_ID, this.parserId);
        map.put(PARSER_NAME, this.parserName);
        map.put(CLASS_NAME, this.className);
        map.put(JAR_STORAGE_PATH, this.jarStoragePath);
        map.put(SCHEMA, this.parserSchema.toString()); //TODO this needs to be toJson
        map.put(VERSION, this.version);
        map.put(TIMESTAMP, this.timestamp);
        return map;
    }

    public Storable fromMap(Map<String, Object> map) {
        this.parserId = (Long) map.get(PARSER_ID);
        this.parserName = (String)  map.get(PARSER_NAME);
        this.className = (String)  map.get(CLASS_NAME);
        this.jarStoragePath = (String)  map.get(JAR_STORAGE_PATH);
        this.parserSchema = Schema.fromString((String) map.get(SCHEMA)); //TODO this needs to be fromJson
        this.version = (Long)map.get(VERSION);
        this.timestamp = (Long)  map.get(TIMESTAMP);
        return this;
    }

    public Long getParserId() {
        return parserId;
    }

    public void setParserId(Long parserId) {
        this.parserId = parserId;
    }

    public String getParserName() {
        return parserName;
    }

    public void setParserName(String parserName) {
        this.parserName = parserName;
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
}
