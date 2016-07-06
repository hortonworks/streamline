package com.hortonworks.iotas.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.common.Schema;

import java.io.IOException;
import java.util.Map;

public class Utils {
    /**
     * This method takes in a schema represented as a map and returns a {@link Schema}
     * @param schemaConfig A map representing {@link Schema}
     * @return schema generated from the map argument
     * @throws IOException
     */
    public static Schema getSchemaFromConfig (Map schemaConfig) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String inputSchemaStr = objectMapper.writeValueAsString(schemaConfig);
        Schema schema = objectMapper.readValue(inputSchemaStr, Schema.class);
        return schema;
    }
}
