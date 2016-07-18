package com.hortonworks.iotas.streams.runtime;

import com.hortonworks.iotas.streams.IotasEvent;

import java.io.Serializable;
import java.util.List;

/**
 * Abstraction for transformations on IotasEvent
 */
public interface TransformRuntime extends Serializable {

    /**
     * Transforms an input {@link IotasEvent} and generates zero, one or
     * more events as a result.
     *
     * @param input the input IotasEvent
     * @return the list of events generated from the transformation
     */
    List<IotasEvent> execute(IotasEvent input);

}
