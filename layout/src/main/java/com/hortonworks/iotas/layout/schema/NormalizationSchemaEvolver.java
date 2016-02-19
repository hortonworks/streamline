package com.hortonworks.iotas.layout.schema;

import com.hortonworks.iotas.common.Schema;

import java.util.Map;

// FIXME: NormalizationSchemaEvolver now handles only field mode
// FIXME: should address bulk mode in IOT-64
public class NormalizationSchemaEvolver implements EvolvingSchema {
    @Override
    public Map<String, Schema> apply(String config, Schema inputSchema) throws BadComponentConfigException {
        
        return null;
    }
}
