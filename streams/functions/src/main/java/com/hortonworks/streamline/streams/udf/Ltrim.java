package com.hortonworks.streamline.streams.udf;

import com.hortonworks.streamline.streams.rule.UDF;

/**
 * Trims leading white spaces from the input.
 * See {@link String#trim()}
 */
public class Ltrim implements UDF<String, String> {
    @Override
    public String evaluate(String input) {
        int len = input.length();
        int st = 0;
        while ((st < len) && (input.charAt(st) <= ' ')) {
            st++;
        }
        return (st > 0) ? input.substring(st) : input;
    }
}
