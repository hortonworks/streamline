package com.hortonworks.streamline.streams.udf;

import com.hortonworks.streamline.streams.rule.UDF;

public class Exists implements UDF<Integer, Long> {
    @Override
    public Integer evaluate(Long input) {
        if (input == null) {
            return 0;
        }
        return 1;
    }
}
