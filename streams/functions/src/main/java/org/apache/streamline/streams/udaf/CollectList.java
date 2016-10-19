package org.apache.streamline.streams.udaf;

import org.apache.streamline.streams.rule.UDAF;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects elements within a group and returns the list of aggregated objects
 */
public class CollectList implements UDAF<List<Object>, Object, List<Object>> {
    @Override
    public List<Object> init() {
        return new ArrayList<>();
    }

    @Override
    public List<Object> add(List<Object> aggregate, Object val) {
        aggregate.add(val);
        return aggregate;
    }

    @Override
    public List<Object> result(List<Object> aggregate) {
        return aggregate;
    }
}
