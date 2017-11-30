package com.hortonworks.streamline.streams.sql.runtime;

import com.hortonworks.streamline.streams.StreamlineEvent;

import java.util.ArrayList;
import java.util.List;

public class CorrelatedEventsAwareValues extends Values {
    private List<StreamlineEvent> correlated;

    public CorrelatedEventsAwareValues() {
        this(new ArrayList<>());
    }

    public CorrelatedEventsAwareValues(List<StreamlineEvent> correlated) {
        this.correlated = correlated;
    }

    public CorrelatedEventsAwareValues(List<StreamlineEvent> correlated, Object... vals) {
        super(vals);
        this.correlated = correlated;
    }

    public List<StreamlineEvent> getCorrelated() {
        return correlated;
    }

    public static CorrelatedEventsAwareValues of(List<StreamlineEvent> correlated, Values vals) {
        return new CorrelatedEventsAwareValues(correlated, vals.toArray());
    }

    @Override
    public Object[] toArray() {
        return super.toArray();
    }
}
