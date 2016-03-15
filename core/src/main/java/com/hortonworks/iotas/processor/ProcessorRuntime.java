package com.hortonworks.iotas.processor;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Result;
import com.hortonworks.iotas.common.errors.ProcessingException;

import java.util.List;
import java.util.Map;

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

    /**
     * Initialize any necessary resources needed for the implementation
     * @param config
     */
    void initialize(Map<String, Object> config);

    /**
     * Clean up any necessary resources needed for the implementation
     */
    void cleanup();
}
