package com.hortonworks.streamline.streams.runtime;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.Result;
import com.hortonworks.streamline.streams.exception.ProcessingException;

import java.util.List;
import java.util.Map;

/**
 * Interface for processors to implement for processing messages at runtime
 */
public interface ProcessorRuntime {
     /**
     * Process the {@link StreamlineEvent} and throw a {@link ProcessingException} if an error arises during processing
     * @param event to be processed
     * @return
     * @throws ProcessingException
     */
    List<Result> process (StreamlineEvent event) throws ProcessingException;

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
