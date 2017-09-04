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

package com.hortonworks.streamline.common;

import com.hortonworks.registries.common.Schema;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Converter class to convert an entity (represented as Map&lt;String, Object&gt;) to be conformed to associated schema,
 * or a value (represented as Object) to be conformed to associated type of the field.
 *
 * More clearly, conversion makes the type of each of value be conformed to the type of field.
 * For example, suppose the value is 1 which type is Integer and the type is LONG which type of Java is Long.
 * After conversion, the value will be 1 which type is Long.
 *
 * Currently it does a magic conversion with only BOOLEAN and Number types (BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE).
 * For other types it just works like a type validator.
 */
public final class SchemaValueConverter {
    private SchemaValueConverter() {
    }

    /**
     * Convert an entity to be conformed to associated schema.
     *
     * @param schema {@link Schema} associate to an event
     * @param value an entity which key represents the field
     * @return the converted entity
     * @throws IllegalArgumentException When the fields in value doesn't conform to the schema.
     */
    public static Map<String, Object> convertMap(Schema schema, Map<String, Object> value) {
        Map<String, Schema.Type> fieldToType = schema.getFields().stream()
                .collect(toMap(Schema.Field::getName, Schema.Field::getType));

        List<Schema.Field> requiredFields = schema.getFields().stream()
                .filter(field -> !field.isOptional()).collect(toList());

        List<String> fieldsNotFoundInSchema = value.keySet().stream()
                .filter(f -> !(fieldToType.containsKey(f))).collect(toList());

        List<Schema.Field> requiredFieldsNotFoundInValue = requiredFields.stream()
                .filter(f -> !(value.containsKey(f.getName()))).collect(toList());

        if (!fieldsNotFoundInSchema.isEmpty()) {
            throw new IllegalArgumentException("The value has fields which are not defined in schema: "
                    + fieldsNotFoundInSchema);
        }

        if (!requiredFieldsNotFoundInValue.isEmpty()) {
            throw new IllegalArgumentException("The value doesn't have required fields: " + requiredFieldsNotFoundInValue);
        }

        return value.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, e -> convert(fieldToType.get(e.getKey()), e.getValue())));
    }

    /**
     * Convert a value to be conformed to the type of associated field.
     *
     * @param type {@link Schema.Type} of the field associate to a value
     * @param value a value
     * @return the converted value
     */
    public static Object convert(Schema.Type type, Object value) {
        if (value == null) {
            return null;
        }

        switch (type) {
            case BOOLEAN:
                if (value instanceof Boolean) {
                    return value;
                } else if (value instanceof String) {
                    return Boolean.valueOf((String) value);
                }
                break;

            case BYTE:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLOAT:
            case DOUBLE:
                return convertNumber(type, value);

            case STRING:
                if (value instanceof String) {
                    return value;
                }
                break;

            case BINARY:
                if (value instanceof byte[]) {
                    return value;
                }
                break;

            case NESTED:
                if (value instanceof Map) {
                    return value;
                }
                break;

            case ARRAY:
                if (value instanceof List) {
                    return value;
                }
                break;

        }

        throw new IllegalArgumentException("Cannot convert value " + value + " with Java type class " +
                value.getClass() + " to type " + type);
    }

    private static Object convertNumber(com.hortonworks.registries.common.Schema.Type type, Object value) {
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("Value " + value + " is not a number.");
        }

        switch (type) {
            case BYTE: {
                long longVal = ((Number) value).longValue();
                byte byteVal = ((Number) value).byteValue();

                if (byteVal == longVal) {
                    return byteVal;
                }

                throw new IllegalArgumentException("Value " + value + " too big to be conformed to " + Schema.Type.BYTE);
            }
            case SHORT: {
                long longVal = ((Number) value).longValue();
                short shortVal = ((Number) value).shortValue();

                if (shortVal == longVal) {
                    return shortVal;
                }

                throw new IllegalArgumentException("Value " + value + " too big to be conformed to " + Schema.Type.SHORT);
            }
            case INTEGER: {
                long longVal = ((Number) value).longValue();
                int intVal = ((Number) value).intValue();

                if (intVal == longVal) {
                    return intVal;
                }

                throw new IllegalArgumentException("Value " + value + " too big to be conformed to " + Schema.Type.INTEGER);
            }
            case LONG:
                return ((Number) value).longValue();

            case FLOAT:
                return ((Number) value).floatValue();

            case DOUBLE:
                return ((Number) value).doubleValue();

            default:
                throw new IllegalArgumentException("Type " + type + " is not belong to numbers.");
        }
    }

}
