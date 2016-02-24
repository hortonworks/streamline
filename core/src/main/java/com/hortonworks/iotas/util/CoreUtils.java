package com.hortonworks.iotas.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.storage.Storable;

import java.io.IOException;
import java.util.Map;

/**
 * Utility methods for the core package.
 */
public class CoreUtils {
    public static <T extends Storable> T jsonToStorable(String json, Class<T> clazz) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, clazz);
    }

    public static String storableToJson(Storable storable) throws IOException {
        if(storable != null) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(storable);
        }
        return null;
    }

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
