package com.hortonworks.streamline.common.exception;

import java.util.List;

public class SchemaValidationFailedException extends RuntimeException {
    private SchemaValidationFailedException(String message) {
        super(message);
    }

    public static SchemaValidationFailedException fieldsNotFoundInSchema(List<String> fields) {
        return new SchemaValidationFailedException("The value has fields which are not defined in schema: " + fields);
    }

    public static SchemaValidationFailedException requiredFieldsNotFoundInValue(List<String> fields) {
        return new SchemaValidationFailedException("The value doesn't have required fields: " + fields);
    }

    public static SchemaValidationFailedException nullValueForField(String field) {
        return new SchemaValidationFailedException("Null value for field: " + field);
    }
}
