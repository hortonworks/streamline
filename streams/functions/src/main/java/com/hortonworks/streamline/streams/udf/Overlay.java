package com.hortonworks.streamline.streams.udf;

import com.hortonworks.streamline.streams.rule.UDF3;

/**
 * Replaces a substring of a string starting at 'start' position with a replacement string
 * <p>
 * Note: The first position in string is 1
 * </p>
 */
public class Overlay implements UDF3<String, String, String, Integer> {
    @Override
    public String evaluate(String string, String r, Integer start) {
        if (string == null || r == null) {
            return null;
        }
        return string.substring(0, Math.min(start - 1, string.length()))
                + r
                + string.substring(Math.min(start - 1 + r.length(), string.length()));
    }
}
