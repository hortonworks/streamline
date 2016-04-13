/**
 *
 */
package com.hortonworks.iotas.layout.runtime.splitjoin;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Result;

import java.util.List;
import java.util.Set;

/**
 * Splitter class can be implemented to split the given input event into multiple events for the given output streams.
 *
 */
public interface Splitter {

    /**
     * Splits the given {@code inputEvent} in to multiple events for the given {@code outputStreams}
     *
     * @param inputEvent
     * @param outputStreams
     * @return List of Results which contain split events for the given input event.
     */
    public List<Result> splitEvent(IotasEvent inputEvent, Set<String> outputStreams);
}
