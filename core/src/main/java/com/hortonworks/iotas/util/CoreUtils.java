package com.hortonworks.iotas.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.storage.Storable;

import java.io.IOException;

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
}
