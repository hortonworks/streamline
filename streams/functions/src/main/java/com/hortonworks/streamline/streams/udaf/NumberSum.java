package com.hortonworks.streamline.streams.udaf;

import com.hortonworks.streamline.streams.rule.UDAF;

public class NumberSum implements UDAF<Number, Number, Number> {
    @Override
    public Number init() {
        return 0;
    }

    @Override
    public Number add(Number aggregate, Number val) {
        if (val instanceof Byte) {
            return (byte) (aggregate.byteValue() + val.byteValue());
        } else if (val instanceof Short) {
            return (short) (aggregate.shortValue() + val.shortValue());
        } else if (val instanceof Integer) {
            return aggregate.intValue() + val.intValue();
        } else if (val instanceof Long) {
            return aggregate.longValue() + val.longValue();
        } else if (val instanceof Float) {
            return aggregate.floatValue() + val.floatValue();
        } else if (val instanceof Double) {
            return aggregate.doubleValue() + val.doubleValue();
        }
        throw new IllegalArgumentException("Value type " + val.getClass());
    }

    @Override
    public Number result(Number aggregate) {
        return aggregate;
    }

}
