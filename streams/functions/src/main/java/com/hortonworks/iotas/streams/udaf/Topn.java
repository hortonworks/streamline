package com.hortonworks.iotas.streams.udaf;

import com.hortonworks.iotas.streams.rule.UDAF2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Computes streaming top n values of a group of values
 */
public class Topn<T extends Comparable<T>> implements UDAF2<PriorityQueue<T>, Integer, T, List<T>> {
    @Override
    public PriorityQueue<T> init() {
        return new PriorityQueue<>();
    }

    @Override
    public PriorityQueue<T> add(PriorityQueue<T> aggregate, Integer n, T val) {
        if (n <= 0) {
            return aggregate;
        }
        if (aggregate.size() >= n) {
            if (val.compareTo(aggregate.peek()) > 0) {
                aggregate.remove();
                aggregate.add(val);
            }
        } else {
            aggregate.add(val);
        }
        return aggregate;

    }

    @Override
    public List<T> result(PriorityQueue<T> aggregate) {
        List<T> res = new ArrayList<>(aggregate);
        Collections.reverse(res);
        return res;
    }
}
