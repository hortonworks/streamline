package org.apache.streamline.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.registries.common.Schema;

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
        return objectMapper.readValue(inputSchemaStr, Schema.class);
    }
}
