package com.hortonworks.streamline.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    /**
     * Checks that the given string is not null or empty. Throws an {@link IllegalArgumentException} if it is.
     *
     * @param s   the string
     * @param msg the exception message
     */
    public static void requireNonEmpty(String s, String msg) {
        if (s == null || s.isEmpty()) {
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Generates the next name for the given name and suffix.
     * <p>
     * E.g. {@code getNextName("test-foo", "-foo") returns "test-foo2",
     *      getNextName("test-foo2", "-foo") returns "test-foo3"}
     * </p>
     * @param name the current name
     * @param suffix the suffix without the number part
     * @return the next name
     */
    public static String getNextName(String name, String suffix) {
        Utils.requireNonEmpty(name, "Empty name");
        String prefix;
        String suffixWithNum;
        int num = getNumPart(name, suffix);
        if (num == -1) {
            prefix = name;
            suffixWithNum = suffix;
        } else {
            prefix = getPrefix(name, suffix);
            suffixWithNum = suffix + (num + 1);
        }
        return prefix + suffixWithNum;
    }

    /**
     * Gets the prefix part a string with a given suffix part.
     * <p>
     * E.g. {@code getPrefix("test-foo", "-foo") returns "test",
     *      getPrefix("test-foo2", "-foo") returns "test"}
     * </p>
     * @param name the name
     * @param suffix the suffix
     * @return the prefix
     */
    public static String getPrefix(String name, String suffix) {
        int idx = name.lastIndexOf(suffix);
        return idx == -1 ? name : name.substring(0, idx);
    }

    /**
     * Returns the latest name from a given list of names that matches the given prefix and suffix part.
     * <p>
     *     E.g.
     *     {@code getLatestName(["test-foo2", "test-foo10", "other", "test-foo1"], "test", "-foo") returns "test-foo10"}
     * </p>
     * @param names
     * @param prefix
     * @param suffix
     * @return
     */
    public static Optional<String> getLatestName(Collection<String> names, String prefix, String suffix) {
        return names.stream()
                .filter(n -> n.startsWith(prefix))
                .max((n1, n2) -> Utils.getNumPart(n1, suffix) - Utils.getNumPart(n2, suffix));
    }
    /**
     * Gets the number part a string with a given suffix part.
     * <p>
     *     E.g. {@code getNumPart("test-foo2", "-foo") returns 2,
     *          getNumPart("test", "-foo") returns -1}
     * </p>
     * @param name   the name
     * @param suffix the suffix
     * @return the number or -1 if the given string does not end with the suffix
     */
    public static int getNumPart(String name, String suffix) {
        int idx = name.lastIndexOf(suffix);
        if (idx != -1 && name.substring(idx).matches(String.format("^%s\\d*$", suffix))) {
            String numPart = name.substring(idx + suffix.length());
            return numPart.isEmpty() ? 1 : Integer.parseInt(numPart);
        }
        return -1;
    }

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
