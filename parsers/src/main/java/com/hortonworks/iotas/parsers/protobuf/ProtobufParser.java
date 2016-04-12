package com.hortonworks.iotas.parsers.protobuf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.protobuf.ProtobufFactory;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufField;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.parser.BaseParser;
import com.hortonworks.iotas.exception.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p> A parser for protocol buffer messages that internally uses
 * jackson-dataformat-protobuf.
 * </p>
 * <p>Version 2 of Protocol Buffer is supported.</p>
 */
public class ProtobufParser extends BaseParser {
    private static final String VERSION = "2.0";
    private static final Logger LOG = LoggerFactory.getLogger(ProtobufParser.class);
    private ProtobufSchema protoBufSchema;
    private ObjectMapper mapper;
    private ObjectReader reader;
    private String protoBufSchemaString;
    private Class<?> clazz;
    private Schema schema;
    private List<Field> rootClassFields;
    private boolean fieldLookup;

    /**
     * <p>Version 2 of Protocol Buffer supported.</p>
     */
    public String version() {
        return VERSION;
    }

    /**
     * Construct using the Builder.
     */
    private ProtobufParser() {
    }

    private void init() throws ParserException {
        try {
            protoBufSchema = ProtobufSchemaLoader.std.parse(protoBufSchemaString);
            mapper = new ObjectMapper(new ProtobufFactory());
            reader = mapper.readerFor(clazz).with(protoBufSchema);
            rootClassFields = getFields(protoBufSchema, clazz);
        } catch (IOException ex) {
            throw new ParserException("Error in protobuf parser", ex);
        }

    }

    private List<Field> getFields(ProtobufSchema protoSchema, Class<?> clazz) throws ParserException {
        List<Field> fields = new ArrayList<Field>();
        List<String> fieldNames = new ArrayList<String>();
        for (ProtobufField field : protoSchema.getRootType().fields()) {
            try {
                fields.add(clazz.getField(field.name));
                fieldNames.add(field.name);
            } catch (NoSuchFieldException e) {
                throw new ParserException("Error getting fields from clazz " + clazz, e);
            }
        }
        LOG.debug("Field names {}", fieldNames);
        return fields;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String protoBufSchemaString;
        private Class<?> clazz;
        private Schema schema;
        private boolean fieldLookup = true;

        /**
         * The protobuf schema string. E.g
         * <pre> <code>
         * message Person {
         *   required string name = 1;
         *   required int32 id = 2;
         *   optional string email = 3;
         * }
         * </code> </pre>
         */
        public Builder protoBufSchemaString(String schemaString) {
            this.protoBufSchemaString = schemaString;
            return this;
        }

        /**
         * The class object of the Type to be parsed.
         */
        public Builder clazz(Class<?> clazz) {
            this.clazz = clazz;
            return this;
        }

        /**
         * The {@link Schema}
         */
        public Builder schema(Schema s) {
            this.schema = s;
            return this;
        }

        /**
         * Will return the parsed object as is in the map with key "value". Default behavior
         * is to return a map of the root class field names to field values.
         */
        public Builder noFieldLookup() {
            fieldLookup = false;
            return this;
        }

        public void checkMandatoryFields() throws ParserException {
            if (protoBufSchemaString == null) {
                throw new ParserException("protoBufSchemaString not set.");
            } else if (clazz == null) {
                throw new ParserException("clazz not set.");
            }
        }

        public ProtobufParser build() throws ParserException {
            checkMandatoryFields();
            ProtobufParser parser = new ProtobufParser();
            parser.protoBufSchemaString = this.protoBufSchemaString;
            parser.clazz = this.clazz;
            parser.schema = this.schema;
            parser.fieldLookup = this.fieldLookup;
            parser.init();
            return parser;
        }
    }

    /**
     * TODO: Should we convert the protobuf definition to schema ?
     *
     * @return
     */
    public Schema schema() {
        return this.schema;
    }

    public Map<String, Object> parse(byte[] data) throws ParserException {
        Map<String, Object> res = new HashMap<String, Object>();
        try {
            Object value = reader.readValue(data);
            if (fieldLookup) {
                for (Field field : rootClassFields) {
                    res.put(field.getName(), clazz.getField(field.getName()).get(value));
                }
            } else {
                res.put("value", value);
            }
        } catch (Exception e) {
            throw new ParserException("Error parsing data", e);
        }
        return res;
    }
}
