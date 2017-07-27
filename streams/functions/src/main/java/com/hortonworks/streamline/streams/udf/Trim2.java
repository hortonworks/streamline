package com.hortonworks.streamline.streams.udf;

import com.hortonworks.streamline.streams.rule.UDF2;
import com.hortonworks.streamline.streams.util.Utils;

/**
 * Returns a string with any leading and trailing character removed.
 */
public class Trim2 implements UDF2<String, String, String> {
    @Override
    public String evaluate(String string, String ch) {
        return Utils.trim(string, ch.charAt(0), true, true);
    }
}
