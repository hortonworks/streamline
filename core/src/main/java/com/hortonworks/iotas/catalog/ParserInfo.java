package com.hortonworks.iotas.catalog;

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
    public static final String SCHEMA = "schema";
    public static final String VERSION = "version";
    public static final String TIME_STAMP = "timestamp";

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
    private Schema schema;

    private Long version;

    /**
     * Time at which this parser was created/updated. //TODO may be we need create and update timestamps.
     */
    private Long timeStamp;


    public String getNameSpace() {
        return "parser-info";
    }

    public PrimaryKey getPrimaryKey() {
        Map<Schema.Field, Object> fieldObjectMap = new HashMap<Schema.Field, Object>();
        fieldObjectMap.put(new Schema.Field(PARSER_ID, Schema.Type.LONG), this.parserId);
        return new PrimaryKey(fieldObjectMap);
    }

    public Schema getSchema() {
        return new Schema(
                new Schema.Field(PARSER_ID, Schema.Type.LONG),
                new Schema.Field(PARSER_NAME, Schema.Type.STRING),
                new Schema.Field(CLASS_NAME, Schema.Type.STRING),
                new Schema.Field(JAR_STORAGE_PATH, Schema.Type.STRING),
                new Schema.Field(SCHEMA, Schema.Type.STRING),
                new Schema.Field(VERSION, Schema.Type.LONG),
                new Schema.Field(TIME_STAMP, Schema.Type.LONG)
        );
    }

    public Map toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(PARSER_ID, this.parserId);
        map.put(PARSER_NAME, this.parserName);
        map.put(CLASS_NAME, this.className);
        map.put(JAR_STORAGE_PATH, this.jarStoragePath);
        map.put(SCHEMA, this.schema.toString()); //TODO this needs to be toJson
        map.put(VERSION, this.version);
        map.put(TIME_STAMP, this.timeStamp);
        return map;
    }

    public Storable fromMap(Map<String, Object> map) {
        this.parserId = (Long) map.get(PARSER_ID);
        this.parserName = (String)  map.get(PARSER_NAME);
        this.className = (String)  map.get(CLASS_NAME);
        this.jarStoragePath = (String)  map.get(JAR_STORAGE_PATH);
        this.schema = Schema.fromString((String) map.get(SCHEMA)); //TODO this needs to be fromJson
        this.version = (Long)map.get(VERSION);
        this.timeStamp = (Long)  map.get(TIME_STAMP);
        return this;
    }


    public Long getParserId() {
        return parserId;
    }

    public String getParserName() {
        return parserName;
    }

    public String getClassName() {
        return className;
    }

    public String getJarStoragePath() {
        return jarStoragePath;
    }

    public Long getVersion() {
        return version;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public static class ParserInfoBuilder {
        private Long parserId;
        private String parserName;
        private String className;
        private String jarStoragePath;
        private Schema schema;
        private long version;
        private long timeStamp;

        public ParserInfoBuilder setParserId(Long parserId) {
            this.parserId = parserId;
            return this;
        }

        public ParserInfoBuilder setParserName(String parserName) {
            this.parserName = parserName;
            return this;
        }

        public ParserInfoBuilder setClassName(String className) {
            this.className = className;
            return this;
        }

        public ParserInfoBuilder setJarStoragePath(String jarStoragePath) {
            this.jarStoragePath = jarStoragePath;
            return this;
        }

        public ParserInfoBuilder setSchema(Schema schema) {
            this.schema = schema;
            return this;
        }

        public ParserInfoBuilder setVersion(long version) {
            this.version = version;
            return this;
        }

        public ParserInfoBuilder setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
            return this;
        }

        public ParserInfo createParserInfo() {
            ParserInfo parserInfo =  new ParserInfo();
            parserInfo.parserId = this.parserId;
            parserInfo.parserName = this.parserName;
            parserInfo.className = this.className;
            parserInfo.jarStoragePath = this.jarStoragePath;
            parserInfo.schema = this.schema;
            parserInfo.version = this.version;
            parserInfo.timeStamp = this.timeStamp;
            return parserInfo;
        }
    }
}
