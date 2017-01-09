package com.hortonworks.streamline.streams.udaf;


import com.hortonworks.streamline.streams.rule.UDAF;

public class Mean implements UDAF<StddevOnline, Number, Double> {
    @Override
    public StddevOnline init() {
        return new StddevOnline();
    }

    @Override
    public StddevOnline add(StddevOnline aggregate, Number val) {
        return aggregate.add(val);
    }

    @Override
    public Double result(StddevOnline aggregate) {
        return aggregate.mean();
    }
}
