/**
 *
 */
package com.hortonworks.iotas.layout.design.normalization;

import com.hortonworks.iotas.common.Schema;

import java.io.Serializable;

/**
 * Abstract class for normalization processor configuration.
 */
public abstract class NormalizationConfig implements Serializable {

    private Schema inputSchema;
    private Schema outputSchema;

    public NormalizationConfig(Schema inputSchema) {
        this.inputSchema = inputSchema;
    }

    public enum TYPE {
        /**
         * It represents a configuration of using a bulk script for normalizing input to output schema.
         */
        bulk,

        /**
         * It represents a configuration of using a script for each field for normalizing input to output schema.
         */
        single
    }

    public abstract TYPE getType();

    public Schema getInputSchema() {
        return inputSchema;
    }

    public Schema getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(Schema outputSchema) {
        this.outputSchema = outputSchema;
    }
}
