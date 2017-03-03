package com.hortonworks.streamline.streams.runtime.storm.bolt;

import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.utils.TupleUtils;

/**
 * This class is based on BaseRichBolt, but is aware of tick tuple.
 */
public abstract class BaseTickTupleAwareRichBolt extends BaseRichBolt {
    /**
     * {@inheritDoc}
     *
     * @param tuple the tuple to process.
     */
    @Override
    public void execute(final Tuple tuple) {
        if (TupleUtils.isTick(tuple)) {
            onTickTuple(tuple);
        } else {
            process(tuple);
        }
    }

    /**
     * Process a single tick tuple of input. Tick tuple doesn't need to be acked.
     * It provides default "DO NOTHING" implementation for convenient. Override this method if needed.
     *
     * More details on {@link org.apache.storm.task.IBolt#execute(Tuple)}.
     *
     * @param tuple The input tuple to be processed.
     */
    protected void onTickTuple(final Tuple tuple) {
    }

    /**
     * Process a single non-tick tuple of input. Implementation needs to handle ack manually.
     * More details on {@link org.apache.storm.task.IBolt#execute(Tuple)}.
     *
     * @param tuple The input tuple to be processed.
     */
    protected abstract void process(final Tuple tuple);
}
