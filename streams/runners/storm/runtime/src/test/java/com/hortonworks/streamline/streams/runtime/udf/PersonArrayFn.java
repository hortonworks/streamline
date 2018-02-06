package com.hortonworks.streamline.streams.runtime.udf;

import com.hortonworks.streamline.streams.rule.UDF;

public class PersonArrayFn implements UDF<String, Object> {

    @Override
    public String evaluate(Object input) {
        return input.toString();
    }
}