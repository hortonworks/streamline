/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
/**
 *
 */
package com.hortonworks.streamline.streams.layout.component.impl.normalization;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.hortonworks.streamline.common.Config;
import com.hortonworks.registries.common.Schema;

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
