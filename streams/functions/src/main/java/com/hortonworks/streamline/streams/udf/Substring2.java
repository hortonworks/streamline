package com.hortonworks.streamline.streams.udf;

import com.hortonworks.streamline.streams.rule.UDF3;

/**
 * Returns a substring of given length from a string starting at some position
 * <p>
 *     Note: The first position in string is 1
 * </p>
 */
public class Substring2 implements UDF3<String, String, Integer, Integer> {
    @Override
    public String evaluate(String string, Integer begin, Integer length) {
        return string.substring(begin - 1, Math.min(begin - 1 + length, string.length()));
    }
}
