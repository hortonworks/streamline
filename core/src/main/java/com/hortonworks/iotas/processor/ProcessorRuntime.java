package com.hortonworks.iotas.processor;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Result;

import java.util.List;

/**
 * Interface for processors to implement for processing messages at runtime
 */
public interface ProcessorRuntime {
     /**
     * Process the {@link IotasEvent} and throw a {@link ProcessingException} if an error arises during processing
     * @param iotasEvent to be processed
     * @return
     * @throws ProcessingException
     */
    List<Result> process (IotasEvent iotasEvent) throws ProcessingException;
}
