package com.hortonworks.streamline.streams.runtime.udf;

import com.hortonworks.streamline.streams.rule.UDF2;

import java.util.Map;

public class SquaresFn implements UDF2<Integer, Object, Integer> {

    @Override
    public Integer evaluate(Object input, Integer key) {
        if (input instanceof Map) {
            Object val = ((Map) input).get(key);
            if (val instanceof Integer) {
                return (Integer) val;
            }
        }
        return null;
    }
}