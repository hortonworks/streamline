package com.hortonworks.streamline.streams.udf;

import com.hortonworks.streamline.streams.rule.UDF4;

/**
 * Replaces a substring of a string with a replacement string
 * <p>
 *     Note: The first position in string is 1
 * </p>
 */
public class Overlay2 implements UDF4<String, String, String, Integer, Integer> {
    @Override
    public String evaluate(String string, String r, Integer start, Integer length) {
        if (string == null || r == null) {
            return null;
        }
        return string.substring(0, Math.min(start - 1, string.length()))
                + r
                + string.substring(Math.min(start - 1 + length, string.length()));
    }
}
