package com.hortonworks.iotas.layout.runtime;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Result;

import java.io.Serializable;

/**
 * Runtime abstraction for the action to be taken when a rule matches the condition
 */
public interface ActionRuntime extends Serializable {

    /**
     * Execute the current action and return the Result object.
     * @param input the input IotasEvent
     * @return the result
     */
    Result execute(IotasEvent input);


    /**
     * The stream where the result of this action are sent out
     *
     * @return the stream where the result of this action are sent out
     */
    String getStream();

}
