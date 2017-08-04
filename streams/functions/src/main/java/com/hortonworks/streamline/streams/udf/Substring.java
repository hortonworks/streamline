package com.hortonworks.streamline.streams.udf;

import com.hortonworks.streamline.streams.rule.UDF2;

/**
 * Returns substring of a string starting at some position
 * <p>
 *     Note: The first position in string is 1
 * </p>
 */
public class Substring implements UDF2<String, String, Integer> {
    @Override
    public String evaluate(String string, Integer begin) {
        return string.substring(begin - 1);
    }
}

