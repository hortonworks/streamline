/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
**/
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hortonworks.streamline.streams.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.hortonworks.streamline.common.Schema;
import com.hortonworks.streamline.streams.StreamlineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.hortonworks.streamline.streams.StreamlineEvent.PRIMITIVE_PAYLOAD_FIELD;

/**
 * Utility class to convert
 * <ul>
 * <li> streamline schema to avro schema </li>
 * <li> avro schema to streamline schema </li>
 * </ul>
 */
public class AvroStreamlineSchemaConverter {
    private static final Logger LOG = LoggerFactory.getLogger(AvroStreamlineSchemaConverter.class);

    /**
     * Converts the given {@code avroSchemaText} to streamline schema {@link Schema}.
     * @param avroSchemaText
     * @return streamline schema for the given {@code avroSchemaText}
     * @throws JsonProcessingException if any error occurs in generating json for generated streams schema fields.
     */
    public static String convertAvroSchemaToStreamlineSchema(String avroSchemaText) throws JsonProcessingException {
        org.apache.avro.Schema avroSchema = new org.apache.avro.Schema.Parser().parse(avroSchemaText);
        LOG.debug("Generating streams schema for given avro schema [{}]", avroSchemaText);

        Schema.Field field = generateStreamsSchemaField(avroSchema);
        List<Schema.Field> effFields;
        // root record is expanded directly as streamline schema represents that as the root element schema
        if (field instanceof Schema.NestedField) {
            effFields = ((Schema.NestedField) field).getFields();
        } else {
            effFields = Collections.singletonList(field);
        }

        return new ObjectMapper().writeValueAsString(effFields);
    }

    /**
     * Converts the given {@code streamlineSchemaText} to avro schema.
     * @param streamlineSchemaText
     * @return avro schema for the given streamline schema
     * @throws IOException if any IO error occurs
     */
    public static String convertStreamlineSchemaToAvroSchema(String streamlineSchemaText) throws IOException {
        List<Schema.Field> fields = new ObjectMapper().readValue(streamlineSchemaText, new TypeReference<List<Schema.Field>>() {});
        if(fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("No fields in the given streamlineSchemaText");
        }

        org.apache.avro.Schema avroSchema;
        // check for primitive type schema
        if(fields.size() == 1 && PRIMITIVE_PAYLOAD_FIELD.equals(fields.iterator().next().getName())) {
            avroSchema = generateAvroSchema(fields.iterator().next());
        } else {

            Schema schema = Schema.of(fields);

            // current abstraction of streamline schema does not really map exactly like avro.
            // streamline schema always takes root element of schema as list of fields and those fields can be either primitive or complex.
            // todo get a parity of streamline schema and avro for root representation
            List<org.apache.avro.Schema.Field> avroFields = new ArrayList<>();
            for (Schema.Field field : schema.getFields()) {
                LOG.info("Generating avro schema for field [{}]", field);
                avroFields.add(new org.apache.avro.Schema.Field(field.getName(), generateAvroSchema(field), null, null));
            }
            avroSchema = org.apache.avro.Schema.createRecord("root", null, null, false);
            avroSchema.setFields(avroFields);
        }

        return avroSchema.toString();
    }

    private static org.apache.avro.Schema generateAvroSchema(Schema.Field field) {
        Preconditions.checkNotNull(field, "Given field can not be null");

        org.apache.avro.Schema avroSchema = null;
        switch (field.getType()) {
            case BOOLEAN:
                avroSchema = org.apache.avro.Schema.create(org.apache.avro.Schema.Type.BOOLEAN);
                break;
            case BYTE:
                avroSchema = org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT);
                break;
            case SHORT:
                avroSchema = org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT);
                break;
            case INTEGER:
                avroSchema = org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT);
                break;
            case LONG:
                avroSchema = org.apache.avro.Schema.create(org.apache.avro.Schema.Type.LONG);
                break;
            case FLOAT:
                avroSchema = org.apache.avro.Schema.create(org.apache.avro.Schema.Type.FLOAT);
                break;
            case DOUBLE:
                avroSchema = org.apache.avro.Schema.create(org.apache.avro.Schema.Type.DOUBLE);
                break;
            case STRING:
                avroSchema = org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING);
                break;
            case BINARY:
                avroSchema = org.apache.avro.Schema.create(org.apache.avro.Schema.Type.BYTES);
                break;
            case NESTED:
                List<org.apache.avro.Schema.Field> avroFields = new ArrayList<>();
                for (Schema.Field innerField : ((Schema.NestedField) field).getFields()) {
                    avroFields.add(new org.apache.avro.Schema.Field(innerField.getName(), generateAvroSchema(innerField), null, null));
                }
                avroSchema = org.apache.avro.Schema.createRecord(field.getName(), null, null, false);
                avroSchema.setFields(avroFields);
                break;
            case ARRAY:
                // for array even though we(Schema in streamline registry) support different types of elements in an array, avro expects an array
                // schema to have elements of same type. Hence, for now we will restrict array to have elements of same type. Other option is convert
                // a  streamline Schema Array field to Record in avro. However, with that the issue is that avro Field constructor does not allow a
                // null name. We could potentiall hack it by plugging in a dummy name like arrayfield, but seems hacky so not taking that path
                avroSchema = org.apache.avro.Schema.createArray(generateAvroSchema(((Schema.ArrayField) field).getMembers().get(0)));
                break;
            default:
                throw new IllegalArgumentException("Given schema type is not supported: " + field.getType());
        }

        LOG.debug("Generated avro schema for given streamline field [{}]: [{}]", field, avroSchema);

        return avroSchema;
    }

    private static Schema.Field generateStreamsSchemaField(org.apache.avro.Schema avroSchema) {
        Schema.Field effField = null;
        Schema.Type fieldType = null;
        boolean isPrimitive = true;
        boolean isOptional = false;
        org.apache.avro.Schema.Type avroSchemaType = avroSchema.getType();
        LOG.debug("Schema type: [{}]", avroSchemaType);
        switch (avroSchemaType) {
            case MAP:
                fieldType = Schema.Type.NESTED;
                break;
            case ARRAY:
                isPrimitive = false;
                LOG.debug("Encountered array field and creating respective member fields");
                effField = generateArraySchema(null, avroSchema);
                LOG.debug("Generated array field [{}]", effField);
                break;
            case ENUM:
            case STRING:
                fieldType = Schema.Type.STRING;
                break;
            case BOOLEAN:
                fieldType = Schema.Type.BOOLEAN;
                break;
            case INT:
                fieldType = Schema.Type.INTEGER;
                break;
            case FLOAT:
                fieldType = Schema.Type.FLOAT;
                break;
            case LONG:
                fieldType = Schema.Type.LONG;
                break;
            case DOUBLE:
                fieldType = Schema.Type.DOUBLE;
                break;
            case BYTES:
            case FIXED:
                fieldType = Schema.Type.BINARY;
                break;
            case NULL:
                fieldType = Schema.Type.NESTED;
                break;
            case RECORD:
                isPrimitive = false;
                LOG.debug("Encountered record field and creating respective nested field");
                effField = generateRecordSchema(avroSchema);
                LOG.debug("Generated nested field [{}] for avro fields [{}]", effField);
                break;
            case UNION:
                // supporting unions with null and a type only, viz is optional values for a type.
                fieldType = getEffectiveUnionSchemas(avroSchema);
                isOptional = true;
                break;
            default:
                throw new IllegalArgumentException("Given type " + avroSchema + " is not supported");
        }

        if (isPrimitive) {
            effField = isOptional ? Schema.Field.optional(PRIMITIVE_PAYLOAD_FIELD, fieldType)
                    : Schema.Field.of(PRIMITIVE_PAYLOAD_FIELD, fieldType);
        }

        return effField;
    }

    private static Schema.Type getEffectiveUnionSchemas(org.apache.avro.Schema avroSchema) {
        Schema.Type fieldType;
        List<org.apache.avro.Schema> avroSchemaTypes = avroSchema.getTypes();
        if (avroSchemaTypes.size() > 2) {
            throw new IllegalArgumentException("Unions with more than two types is not supported.");
        }
        org.apache.avro.Schema type1 = avroSchemaTypes.get(0);
        org.apache.avro.Schema type2 = avroSchemaTypes.get(1);
        if (org.apache.avro.Schema.Type.NULL.equals(type1.getType())) {
            fieldType = getStreamsSchemaFieldType(type2);
        } else if (org.apache.avro.Schema.Type.NULL.equals(type2.getType())) {
            fieldType = getStreamsSchemaFieldType(type1);
        } else {
            throw new IllegalArgumentException("One of the two types in union must be `null`.");
        }
        return fieldType;
    }

    private static Schema.ArrayField generateArraySchema(String name, org.apache.avro.Schema avroSchema) {
        return Schema.ArrayField.of(name, generateStreamsSchemaField(avroSchema.getElementType()));
    }

    private static Schema.NestedField generateRecordSchema(org.apache.avro.Schema avroSchema) {
        return generateRecordSchema(avroSchema, avroSchema.getName());
    }

    private static Schema.NestedField generateRecordSchema(org.apache.avro.Schema avroSchema, String name) {
        List<org.apache.avro.Schema.Field> avroFields = avroSchema.getFields();
        List<Schema.Field> fields = new ArrayList<>();
        for (org.apache.avro.Schema.Field avroField : avroFields) {
            if (avroField.schema().getType() == org.apache.avro.Schema.Type.RECORD) {
                LOG.debug("Encountered record field and creating respective nested fields");
                fields.add(generateRecordSchema(avroField.schema(), avroField.name()));
            } else if (avroField.schema().getType() == org.apache.avro.Schema.Type.ARRAY) {
                LOG.debug("Encountered array field and creating respective member fields");
                fields.add(generateArraySchema(avroField.name(), avroField.schema()));
            } else {
                boolean isOptional = org.apache.avro.Schema.Type.UNION.equals(avroField.schema().getType());
                Schema.Field field = isOptional ? Schema.Field.optional(avroField.name(), getStreamsSchemaFieldType(avroField.schema()))
                                                : Schema.Field.of(avroField.name(), getStreamsSchemaFieldType(avroField.schema()));
                fields.add(field);
            }
        }

        return Schema.NestedField.optional(name, fields);
    }

    private static Schema.Type getStreamsSchemaFieldType(org.apache.avro.Schema avroSchema) {
        org.apache.avro.Schema.Type avroSchemaType = avroSchema.getType();
        Schema.Type fieldType;
        switch (avroSchemaType) {
            case MAP:
                fieldType = Schema.Type.NESTED;
                break;
            case ARRAY:
                fieldType = Schema.Type.ARRAY;
                break;
            case ENUM:
            case STRING:
                fieldType = Schema.Type.STRING;
                break;
            case BOOLEAN:
                fieldType = Schema.Type.BOOLEAN;
                break;
            case INT:
                fieldType = Schema.Type.INTEGER;
                break;
            case FLOAT:
                fieldType = Schema.Type.FLOAT;
                break;
            case LONG:
                fieldType = Schema.Type.LONG;
                break;
            case DOUBLE:
                fieldType = Schema.Type.DOUBLE;
                break;
            case BYTES:
            case FIXED:
                fieldType = Schema.Type.BINARY;
                break;
            case RECORD:
            case NULL:
                fieldType = Schema.Type.NESTED;
                break;
            case UNION:
                fieldType = getEffectiveUnionSchemas(avroSchema);
                break;
            default:
                throw new IllegalArgumentException("Given type " + avroSchema + " is not supported");
        }

        return fieldType;
    }

}
