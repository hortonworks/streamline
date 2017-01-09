package org.apache.streamline.streams.runtime;

import org.apache.streamline.streams.exception.ConfigException;

import java.util.Map;

/**
 * An interface for supporting custom processor components in an Streamline topology
 */
public interface CustomProcessorRuntime extends ProcessorRuntime {

    /**
     * Validate configuration provided and throw a {@link ConfigException} if missing or invalid configuration
     * @throws ConfigException
     * @param config
     */
    void validateConfig(Map<String, Object> config) throws ConfigException;

}
