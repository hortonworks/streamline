package com.hortonworks.streamline.streams.udf;

import com.hortonworks.streamline.streams.rule.UDF;

/**
 * Trims trailing white spaces from the input.
 * See {@link String#trim()}
 */
public class Rtrim implements UDF<String, String> {
    @Override
    public String evaluate(String input) {
        int len = input.length();
        while ((len > 0) && (input.charAt(len - 1) <= ' ')) {
            len--;
        }
        return (len < input.length()) ? input.substring(0, len) : input;
    }
}
