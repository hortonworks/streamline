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


package com.hortonworks.streamline.streams.layout.component.impl.normalization;

import com.hortonworks.registries.common.Schema;

import java.io.Serializable;

/**
 * This class represents design time configuration of a transformer which converts a given input field value to an output
 * field value using a converter script.
 * For example, input field with name `temp` (in degree centigrade). It should be mapped to output field with name
 * `temperature` (in degree fahrenheit) by passing a groovy script which does the transformation.
 *
 */
public class Transformer implements Serializable {
    private Schema.Field inputField;
    private Schema.Field outputField;
    private String converterScript;

    public Transformer() {
    }

    public Transformer(Schema.Field inputField, Schema.Field outputField) {
        this.inputField = inputField;
        this.outputField = outputField;
    }

    public Schema.Field getInputField() {
        return inputField;
    }

    public void setInputField(Schema.Field inputField) {
        this.inputField = inputField;
    }

    public Schema.Field getOutputField() {
        return outputField;
    }

    public void setOutputField(Schema.Field outputField) {
        this.outputField = outputField;
    }

    public String getConverterScript() {
        return converterScript;
    }

    public void setConverterScript(String converterScript) {
        this.converterScript = converterScript;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transformer)) return false;

        Transformer that = (Transformer) o;

        if (inputField != null ? !inputField.equals(that.inputField) : that.inputField != null) return false;
        if (outputField != null ? !outputField.equals(that.outputField) : that.outputField != null) return false;
        return !(converterScript != null ? !converterScript.equals(that.converterScript) : that.converterScript != null);

    }

    @Override
    public int hashCode() {
        int result = inputField != null ? inputField.hashCode() : 0;
        result = 31 * result + (outputField != null ? outputField.hashCode() : 0);
        result = 31 * result + (converterScript != null ? converterScript.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Transformer{" +
                "inputField=" + inputField +
                ", outputField=" + outputField +
                ", converterScript='" + converterScript + '\'' +
                '}';
    }
}
