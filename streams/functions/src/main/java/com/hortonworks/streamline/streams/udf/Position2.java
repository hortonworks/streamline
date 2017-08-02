package com.hortonworks.streamline.streams.udf;

import com.hortonworks.streamline.streams.rule.UDF3;

/**
 * Returns the position of the first occurrence of sub string in  a string starting the search from an index.
 * <p>
 * Note: The first position in string is 1 and if substring is not found within string, the function will return 0
 * </p>
 */
public class Position2 implements UDF3<Integer, String, String, Integer> {
    @Override
    public Integer evaluate(String string, String sub, Integer fromIndex) {
        return string.indexOf(sub, fromIndex - 1) + 1;
    }
}
