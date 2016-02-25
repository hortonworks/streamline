package com.hortonworks.iotas.layout.schema;

import com.hortonworks.iotas.common.Schema;

import java.io.IOException;
import java.util.Map;

public interface EvolvingSchema {
    // should return map of (stream, next schema)
    Map<String, Schema> apply(final String config, Schema inputSchema) throws BadComponentConfigException;
}
