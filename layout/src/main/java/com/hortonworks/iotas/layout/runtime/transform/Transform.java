package com.hortonworks.iotas.layout.runtime.transform;

import com.hortonworks.iotas.common.IotasEvent;

import java.io.Serializable;
import java.util.List;

/**
 * Abstraction for transformations on IotasEvent
 */
public interface Transform extends Serializable {
    /**
     * Transforms an input {@link IotasEvent} and generates zero, one or
     * more events as a result.
     *
     * @param input the input IotasEvent
     * @return the list of events generated from the transformation
     */
    List<IotasEvent> execute(IotasEvent input);
}
