package com.hortonworks.iotas.streams.udaf;

import com.hortonworks.iotas.streams.rule.UDAF;

public class Variance implements UDAF<StddevOnline, Double, Double> {
    @Override
    public StddevOnline init() {
        return new StddevOnline();
    }

    @Override
    public StddevOnline add(StddevOnline aggregate, Double val) {
        return aggregate.add(val);
    }

    @Override
    public Double result(StddevOnline aggregate) {
        return aggregate.variance();
    }
}
