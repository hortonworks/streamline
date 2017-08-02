package com.hortonworks.streamline.streams.udf;

import com.hortonworks.streamline.streams.rule.UDF;

/**
 * Returns a string with any leading and trailing whitespace removed.
 */
public class Trim implements UDF<String, String> {
    @Override
    public String evaluate(String input) {
        return input.trim();
    }
}
