/**
 *
 */
package org.apache.streamline.streams.layout.component.impl.normalization;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.streamline.common.Config;
import org.apache.streamline.common.Schema;

/**
 * Base class for normalization processor configuration.
 */
@JsonTypeInfo(use= JsonTypeInfo.Id.CLASS, include= JsonTypeInfo.As.PROPERTY, property="__type")
public class NormalizationConfig extends Config {

    private Schema inputSchema;

    private NormalizationConfig() {
    }

    public NormalizationConfig(Schema inputSchema) {
        this.inputSchema = inputSchema;
    }

    public Schema getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(Schema inputSchema) {
        this.inputSchema = inputSchema;
    }
}
