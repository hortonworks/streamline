package com.hortonworks.iotas.parser;

import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.Storable;
import com.hortonworks.iotas.storage.Id;

import java.util.HashMap;
import java.util.Map;

public class ParserInfo implements Storable {
    public static final String PARSER_ID = "parserId";
    public static final String PARSER_NAME = "parserName";
    public static final String CLASS_NAME = "className";
    public static final String JAR_STORAGE_PATH = "jarStoragePath";
    public static final String TIME_STAMP = "timestamp";

    private String parserId;
    private String parserName;
    private String className;
    private String jarStoragePath;
    private long timeStamp;

    public String getNameSpace() {
        return "parser-info";
    }

    public Id getId() {
        Map<Schema.Field, Object> fieldObjectMap = new HashMap<Schema.Field, Object>();
        fieldObjectMap.put(new Schema.Field(PARSER_ID, Schema.Type.STRING), this.parserId);
        return new Id(fieldObjectMap);
    }

    public Schema getSchema() {
        return new Schema(
                new Schema.Field(PARSER_ID, Schema.Type.STRING),
                new Schema.Field(PARSER_NAME, Schema.Type.STRING),
                new Schema.Field(CLASS_NAME, Schema.Type.STRING),
                new Schema.Field(JAR_STORAGE_PATH, Schema.Type.STRING),
                new Schema.Field(TIME_STAMP, Schema.Type.LONG)
        );
    }

    public Map toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(PARSER_ID, this.parserId);
        map.put(PARSER_NAME, this.parserName);
        map.put(CLASS_NAME, this.className);
        map.put(JAR_STORAGE_PATH, this.jarStoragePath);
        map.put(TIME_STAMP, this.timeStamp);
        return map;
    }

    public Storable fromMap(Map<String, Object> map) {
        this.parserId = (String) map.get(PARSER_ID);
        this.parserName = (String)  map.get(PARSER_NAME);
        this.className = (String)  map.get(CLASS_NAME);
        this.jarStoragePath = (String)  map.get(JAR_STORAGE_PATH);
        this.timeStamp = (Long)  map.get(TIME_STAMP);
        return this;
    }

    public static class ParserInfoBuilder {
        private String parserId;
        private String parserName;
        private String className;
        private String jarStoragePath;
        private long timeStamp;

        public ParserInfoBuilder setParserId(String parserId) {
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

        public ParserInfoBuilder setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
            return this;
        }

        public ParserInfo createParserInfo() {
            ParserInfo parserInfo = new ParserInfo();
            parserInfo.parserId = this.parserId;
            parserInfo.parserName = this.parserName;
            parserInfo.className = this.className;
            parserInfo.jarStoragePath = this.jarStoragePath;
            parserInfo.timeStamp = this.timeStamp;
            return parserInfo;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParserInfo)) return false;

        ParserInfo that = (ParserInfo) o;

        if (timeStamp != that.timeStamp) return false;
        if (!parserId.equals(that.parserId)) return false;
        if (!parserName.equals(that.parserName)) return false;
        if (!className.equals(that.className)) return false;
        return jarStoragePath.equals(that.jarStoragePath);
    }

    @Override
    public int hashCode() {
        int result = parserId.hashCode();
        result = 31 * result + parserName.hashCode();
        result = 31 * result + className.hashCode();
        result = 31 * result + jarStoragePath.hashCode();
        result = 31 * result + (int) (timeStamp ^ (timeStamp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "ParserInfo{" +
                "parserId='" + parserId + '\'' +
                ", parserName='" + parserName + '\'' +
                ", className='" + className + '\'' +
                ", jarStoragePath='" + jarStoragePath + '\'' +
                ", timeStamp=" + timeStamp +
                '}';
    }
}
