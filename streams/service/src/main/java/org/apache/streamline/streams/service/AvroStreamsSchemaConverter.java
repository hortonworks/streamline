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
package org.apache.streamline.streams.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.common.Schema;
import org.apache.streamline.streams.IotasEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public class AvroStreamsSchemaConverter {
    private static final Logger LOG = LoggerFactory.getLogger(AvroStreamsSchemaConverter.class);

    public String convertAvro(String schemaText) throws JsonProcessingException {
        org.apache.avro.Schema avroSchema = new org.apache.avro.Schema.Parser().parse(schemaText);
        LOG.debug("Generating streams schema for given avro schema [{}]", schemaText);

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

    private Schema.Field generateStreamsSchemaField(org.apache.avro.Schema avroSchema) {
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
                org.apache.avro.Schema.Type elementType = avroSchema.getElementType().getType();
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
            effField = isOptional ? Schema.Field.optional(IotasEvent.PRIMITIVE_PAYLOAD_FIELD, fieldType)
                    : Schema.Field.of(IotasEvent.PRIMITIVE_PAYLOAD_FIELD, fieldType);
        }

        return effField;
    }

    private Schema.Type getEffectiveUnionSchemas(org.apache.avro.Schema avroSchema) {
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

    private Schema.NestedField generateRecordSchema(org.apache.avro.Schema avroSchema) {
        List<org.apache.avro.Schema.Field> avroFields = avroSchema.getFields();
        List<Schema.Field> fields = new ArrayList<>();
        for (org.apache.avro.Schema.Field avroField : avroFields) {
            if (avroField.schema().getType() == org.apache.avro.Schema.Type.RECORD) {
                LOG.debug("Encountered record field and creating respective nested fields");
                fields.add(generateRecordSchema(avroField.schema()));
            } else {
                boolean isOptional = org.apache.avro.Schema.Type.UNION.equals(avroField.schema().getType());
                Schema.Field field = isOptional ? Schema.Field.optional(avroField.name(), getStreamsSchemaFieldType(avroField.schema()))
                                                : Schema.Field.of(avroField.name(), getStreamsSchemaFieldType(avroField.schema()));
                fields.add(field);
            }
        }

        return Schema.NestedField.optional(avroSchema.getName(), fields);
    }

    private Schema.Type getStreamsSchemaFieldType(org.apache.avro.Schema avroSchema) {
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
