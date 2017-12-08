package com.hortonworks.streamline.streams.sql.runtime;

import com.hortonworks.streamline.streams.StreamlineEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * This class extends Values to track correlated (in fact, parent) Streamline events.
 * <p/>
 * Normally we can track the parent event(s) while applying operations on event(s),
 * but it doesn't hold true for operations in script (Groovy, SQL).
 * <p/>
 * This class enables script to forward Values with correlated events, so that
 * script can execute operations and include correlated events in each phase's result, and
 * Streamline can associate correlated events with result when script execution is complete.
 */
public class CorrelatedValues extends Values {
    private List<StreamlineEvent> correlated;

    public CorrelatedValues() {
        this(new ArrayList<>());
    }

    public CorrelatedValues(List<StreamlineEvent> correlated) {
        this.correlated = correlated;
    }

    public CorrelatedValues(List<StreamlineEvent> correlated, Object... vals) {
        super(vals);
        this.correlated = correlated;
    }

    public List<StreamlineEvent> getCorrelated() {
        return correlated;
    }

    public static CorrelatedValues of(List<StreamlineEvent> correlated, Values vals) {
        return new CorrelatedValues(correlated, vals.toArray());
    }

    @Override
    public Object[] toArray() {
        return super.toArray();
    }
}
