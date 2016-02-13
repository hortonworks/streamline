package com.hortonworks.iotas.processor;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Result;

import java.util.List;
import java.util.Map;

/**
 * An interface for supporting custom processor components in an IoTaS topology
 */
public interface CustomProcessor extends ProcessorRuntime {
    /**
     * Provide the configuration to the {@link CustomProcessor} implementation.
     * @param config configuration object capturing values needed to configure the implementation
     */
    void config (Map<String, Object> config);

    /**
     * Validate configuration provided and throw a {@link ConfigException} if missing or invalid configuration
     * @throws ConfigException
     */
    void validateConfig () throws ConfigException;

    void initialize ();

    void cleanup ();

}
