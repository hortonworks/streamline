package com.hortonworks.iotas.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

/**
 * Created by pshah on 9/30/15.
 */
public class JsonSchemaValidator {
    /**
     *
     * @param jsonSchema URL representing a resource that is a schema json
     * @param jsonData JSON.stringify(json) string representing
     *                       data json to be validated against the schema
     * @return
     */

    public static boolean isValidJsonAsPerSchema (URL jsonSchema,
                                                  String jsonData) throws IOException, ProcessingException {
        boolean result = false;
        final JsonNode schemaNode = JsonLoader.fromURL(jsonSchema);
        final JsonNode dataNode = JsonLoader.fromString(jsonData);
        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        final JsonSchema schema = factory.getJsonSchema(schemaNode);
        result = schema.validate(dataNode).isSuccess();
        //throw new RuntimeException("Error while validating json: ", ex);
        return result;
    }

}
