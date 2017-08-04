package com.hortonworks.streamline.streams.udf;

import com.hortonworks.streamline.streams.rule.UDF2;

/**
 * Returns the position of the first occurrence of sub string in  a string.
 * <p>
 * Note: The first position in string is 1 and if substring is not found within string, the function will return 0
 * </p>
 */
public class Position implements UDF2<Integer, String, String> {
    @Override
    public Integer evaluate(String string, String sub) {
        return string.indexOf(sub) + 1;
    }
}
