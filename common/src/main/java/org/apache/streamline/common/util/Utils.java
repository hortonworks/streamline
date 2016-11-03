package org.apache.streamline.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.common.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
    /**
     * This method takes in a schema represented as a map and returns a {@link Schema}
     * @param schemaConfig A map representing {@link Schema}
     * @return schema generated from the map argument
     * @throws IOException
     */
    public static Schema getSchemaFromConfig (Map schemaConfig) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return getSchemaFromConfig(objectMapper.writeValueAsString(schemaConfig));
    }
    /**
     * This method takes in a schema represented as a json string and returns a {@link Schema}
     * @param schemaConfig A map representing {@link Schema}
     * @return schema generated from the string argument
     * @throws IOException
     */
    public static Schema getSchemaFromConfig (String schemaConfig) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(schemaConfig, Schema.class);
    }

    /**
     * Deserialize a json string to a java object
     * @param json string to deserialize
     * @param classType class of java object for deserialization
     * @param <T>
     * @return
     */
    public static <T> T createObjectFromJson(String json, Class<T> classType) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, classType);
        } catch (IOException e) {
            LOG.error("Error while deserializing json string {} to {}", json, classType, e);
        }
        return null;
    }
}
