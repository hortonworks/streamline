package com.hortonworks.iotas.util;

import com.google.common.collect.Maps;
import com.hortonworks.iotas.common.Schema;

import java.util.Map;

public class SchemaNamespaceUtil {
    public static Schema applyNamespace(String namespace, Schema origin) {
        Schema.SchemaBuilder schemaBuilder = new Schema.SchemaBuilder();
        for (Schema.Field field : origin.getFields()) {
            String newFieldName = applyNamespaceToFieldName(namespace, field.getName());
            Schema.Type type = field.getType();

            if (field.isOptional()) {
                schemaBuilder.field(Schema.Field.optional(newFieldName, type));
            } else {
                schemaBuilder.field(Schema.Field.of(newFieldName, type));
            }
        }

        return schemaBuilder.build();
    }

    public static Map<String, Object> applyNamespace(String namespace, Map<String, Object> fieldAndValues) {
        Map<String, Object> nameSpaceApplied = Maps.newHashMap();

        for (Map.Entry<String, Object> fieldToValue : fieldAndValues.entrySet()) {
            String fieldName = fieldToValue.getKey();
            Object value = fieldToValue.getValue();

            nameSpaceApplied.put(applyNamespaceToFieldName(namespace, fieldName), value);
        }

        return nameSpaceApplied;
    }

    private static String applyNamespaceToFieldName(String namespace, String fieldName) {
        return String.format("%s.%s", namespace, fieldName);
    }
}
