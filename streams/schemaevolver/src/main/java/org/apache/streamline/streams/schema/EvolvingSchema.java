package org.apache.streamline.streams.schema;

import org.apache.streamline.streams.layout.component.Stream;
import org.apache.streamline.streams.schema.exception.BadComponentConfigException;

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
