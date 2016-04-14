package com.hortonworks.iotas.layout.schema;

import com.hortonworks.iotas.layout.design.component.Stream;

import java.util.Set;

/**
 * An interface for supporting 'Schema Evolution' for processor type components
 */
public interface EvolvingSchema {

    /**
     * Simulate changes of schema based on component config and input stream.
     *
     * @param config JSON representation of component config within topology
     * @param inputStream input stream
     * @return set of output streams this component is expected to provide
     * @throws BadComponentConfigException
     */
    Set<Stream> apply(final String config, Stream inputStream) throws BadComponentConfigException;
}
