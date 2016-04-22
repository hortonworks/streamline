package com.hortonworks.iotas.processor;

import com.hortonworks.iotas.common.errors.ConfigException;

import java.util.Map;

/**
 * An interface for supporting custom processor components in an IoTaS topology
 */
public interface CustomProcessor extends ProcessorRuntime {

    /**
     * Validate configuration provided and throw a {@link ConfigException} if missing or invalid configuration
     * @throws ConfigException
     * @param config
     */
    void validateConfig(Map<String, Object> config) throws ConfigException;

}
