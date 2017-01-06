package com.hortonworks.streamline.streams.runtime;

import com.hortonworks.streamline.streams.StreamlineEvent;

import java.io.Serializable;
import java.util.List;

/**
 * Abstraction for transformations on StreamlineEvent
 */
public interface TransformRuntime extends Serializable {

    /**
     * Transforms an input {@link StreamlineEvent} and generates zero, one or
     * more events as a result.
     *
     * @param input the input StreamlineEvent
     * @return the list of events generated from the transformation
     */
    List<StreamlineEvent> execute(StreamlineEvent input);

}
