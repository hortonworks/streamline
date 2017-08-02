package com.hortonworks.streamline.streams.udf;

import com.hortonworks.streamline.streams.rule.UDF2;
import com.hortonworks.streamline.streams.util.Utils;

/**
 * Trims leading character 'ch' from the input.
 */
public class Ltrim2 implements UDF2<String, String, String> {

    @Override
    public String evaluate(String string, String ch) {
        return Utils.trim(string, ch.charAt(0), true, false);
    }
}
