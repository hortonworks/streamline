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
package com.hortonworks.streamline.streams.catalog.processor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.common.ComponentUISpecification;
import com.hortonworks.streamline.common.util.Utils;
import com.hortonworks.streamline.streams.catalog.topology.TopologyComponentBundle;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;

import java.io.IOException;
import java.util.*;

/**
 * Class representing information about a custom processor helping in the registration process
 */
public class CustomProcessorInfo {
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String JAR_FILE_NAME = "jarFileName";
    public static final String INPUT_SCHEMA = "inputSchema";
    public static final String OUTPUT_SCHEMA = "outputSchema";
    public static final String CUSTOM_PROCESSOR_IMPL = "customProcessorImpl";
    public static final String DIGEST = "digest";

    private static final Set<String> PROPERTY_KEYS;
    static {
        HashSet<String> result = new HashSet<>();
        result.add(NAME);
        result.add(DESCRIPTION);
        result.add(JAR_FILE_NAME);
        result.add(INPUT_SCHEMA);
        result.add(OUTPUT_SCHEMA);
        result.add(CUSTOM_PROCESSOR_IMPL);
        result.add(DIGEST);
        PROPERTY_KEYS = result;
    }

    private String streamingEngine;
    private String name;
    private String description;
    private String jarFileName;
    private ComponentUISpecification topologyComponentUISpecification;
    private Schema inputSchema;
    private Schema outputSchema;
    private String customProcessorImpl;

    /**
     * The jar file digest which can be used to de-dup jar files.
     * If a newly submitted jar's digest matches with that of an already
     * existing jar, we just use that jar file path rather than storing a copy.
     */
    private String digest;

    /* jackson needs a default non args constructor*/
    private CustomProcessorInfo() {
    }

    public CustomProcessorInfo(String name,
                               String description,
                               String streamingEngine,
                               String jarFileName,
                               String customProcessorImpl,
                               Schema inputSchema,
                               Schema outputSchema,
                               ComponentUISpecification topologyComponentUISpecification,
                               String digest) {
        this.name = name;
        this.description = description;
        this.streamingEngine = streamingEngine;
        this.jarFileName = jarFileName;
        this.customProcessorImpl = customProcessorImpl;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.topologyComponentUISpecification = topologyComponentUISpecification;
        this.digest = digest;
    }

    public Schema getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(Schema outputSchema) {
        this.outputSchema = outputSchema;
    }

    public String getStreamingEngine() {
        return streamingEngine;
    }

    public void setStreamingEngine(String streamingEngine) {
        this.streamingEngine = streamingEngine;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonIgnore
    public String getJarFileName() {
        return jarFileName;
    }

    @JsonIgnore
    public void setJarFileName(String jarFileName) {
        this.jarFileName = jarFileName;
    }

    public Schema getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(Schema inputSchema) {
        this.inputSchema = inputSchema;
    }

    public String getCustomProcessorImpl() {
        return customProcessorImpl;
    }

    public void setCustomProcessorImpl(String customProcessorImpl) {
        this.customProcessorImpl = customProcessorImpl;
    }

    public ComponentUISpecification getTopologyComponentUISpecification() {
        return topologyComponentUISpecification;
    }

    public void setTopologyComponentUISpecification(ComponentUISpecification componentUISpecification) {
        this.topologyComponentUISpecification = componentUISpecification;
    }

    @JsonIgnore
    public String getDigest() {
        return digest;
    }

    @JsonIgnore
    public void setDigest (String digest) {
        this.digest = digest;
    }

    public static CustomProcessorInfo fromTopologyComponentBundle(TopologyComponentBundle topologyComponentBundle) throws IOException {

        Preconditions.checkNotNull(topologyComponentBundle, "topologyComponentBundle can not bre null");

        ComponentUISpecification topologyComponentUISpecification = topologyComponentBundle.getTopologyComponentUISpecification();
        List<ComponentUISpecification.UIField> uiFields = topologyComponentUISpecification.getFields();
        Map<String, String> config = getPropertiesFromUIFields(uiFields);

        return new CustomProcessorInfo(config.get(NAME),
                config.get(DESCRIPTION),
                topologyComponentBundle.getStreamingEngine(),
                config.get(JAR_FILE_NAME),
                config.get(CUSTOM_PROCESSOR_IMPL),
                Utils.getSchemaFromConfig(config.get(INPUT_SCHEMA)),
                Utils.getSchemaFromConfig(config.get(OUTPUT_SCHEMA)),
                getCustomProcessorUISpecification(topologyComponentUISpecification),
                config.get(DIGEST));
    }


    @Override
    public String toString() {
        return "CustomProcessorInfo{" +
                "streamingEngine='" + streamingEngine + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", jarFileName='" + jarFileName + '\'' +
                ", topologyComponentUISpecification='" + topologyComponentUISpecification+ '\'' +
                ", inputSchema=" + inputSchema +
                ", outputSchema=" + outputSchema +
                ", digest=" + digest +
                ", customProcessorImpl='" + customProcessorImpl + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CustomProcessorInfo that = (CustomProcessorInfo) o;

        if (streamingEngine != null ? !streamingEngine.equals(that.streamingEngine) : that.streamingEngine != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (jarFileName != null ? !jarFileName.equals(that.jarFileName) : that.jarFileName != null) return false;
        if (topologyComponentUISpecification != null ? !topologyComponentUISpecification.equals(that.topologyComponentUISpecification) : that
                .topologyComponentUISpecification != null)
            return false;
        if (inputSchema != null ? !inputSchema.equals(that.inputSchema) : that.inputSchema != null) return false;
        if (outputSchema != null ? !outputSchema.equals(that.outputSchema) : that.outputSchema != null) return false;
        if (digest != null ? !digest.equals(that.digest) : that.digest != null) return false;
        return !(customProcessorImpl != null ? !customProcessorImpl.equals(that.customProcessorImpl) : that.customProcessorImpl != null);

    }

    @Override
    public int hashCode() {
        int result = streamingEngine != null ? streamingEngine.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (jarFileName != null ? jarFileName.hashCode() : 0);
        result = 31 * result + (topologyComponentUISpecification != null ? topologyComponentUISpecification.hashCode() : 0);
        result = 31 * result + (inputSchema != null ? inputSchema.hashCode() : 0);
        result = 31 * result + (outputSchema != null ? outputSchema.hashCode() : 0);
        result = 31 * result + (digest != null ? digest.hashCode() : 0);
        result = 31 * result + (customProcessorImpl != null ? customProcessorImpl.hashCode() : 0);
        return result;
    }

    public TopologyComponentBundle toTopologyComponentBundle () throws IOException {
        TopologyComponentBundle result = new TopologyComponentBundle();
        result.setTimestamp(System.currentTimeMillis());
        result.setType(TopologyComponentBundle.TopologyComponentType.PROCESSOR);
        result.setSubType(TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_SUB_TYPE);
        result.setStreamingEngine(this.streamingEngine);
        if (TopologyLayoutConstants.STORM_STREAMING_ENGINE.equals(this.streamingEngine)) {
            result.setName("customProcessorBoltComponent");
        }
        result.setBuiltin(true);
        result.setTransformationClass("com.hortonworks.streamline.streams.layout.storm.CustomProcessorBoltFluxComponent");
        List<ComponentUISpecification.UIField> uiFields = new ArrayList<>();
        uiFields.addAll(getCustomProcessorUIFieldsWithPrefix());
        uiFields.add(this.createUIField(TopologyLayoutConstants.JSON_KEY_PARALLELISM, TopologyLayoutConstants.JSON_KEY_PARALLELISM, true, true,
                TopologyLayoutConstants.JSON_KEY_PARALLELISM_TOOLTIP, ComponentUISpecification.UIFieldType.NUMBER.NUMBER, 1));
        uiFields.add(this.createUIField(NAME, NAME, false, false, "Custom processor name", ComponentUISpecification.UIFieldType.STRING, this.name));
        uiFields.add(this.createUIField(DESCRIPTION, DESCRIPTION, false, false, "Custom processor description", ComponentUISpecification.UIFieldType
                .STRING, this.description));
        uiFields.add(this.createUIField(JAR_FILE_NAME, JAR_FILE_NAME, false, false, "Custom processor jar file", ComponentUISpecification.UIFieldType
                .STRING, this.jarFileName));
        uiFields.add(this.createUIField(CUSTOM_PROCESSOR_IMPL, CUSTOM_PROCESSOR_IMPL, false, false, "Custom processor interface implementation class",
                ComponentUISpecification.UIFieldType.STRING, this.customProcessorImpl));
        ObjectMapper objectMapper = new ObjectMapper();
        uiFields.add(this.createUIField(INPUT_SCHEMA, INPUT_SCHEMA, true, false, "Custom processor input schema", ComponentUISpecification
                .UIFieldType.STRING, objectMapper.writeValueAsString(this.inputSchema)));
        uiFields.add(this.createUIField(OUTPUT_SCHEMA, OUTPUT_SCHEMA, true, false, "Custom processor output schema",
                ComponentUISpecification.UIFieldType.STRING, objectMapper.writeValueAsString(this.outputSchema)));
        uiFields.add(this.createUIField(DIGEST, DIGEST, false, false, "MD5 digest of the jar file for this CP implementation",
                ComponentUISpecification.UIFieldType.STRING, this.digest));
        ComponentUISpecification componentUISpecification = new ComponentUISpecification();
        componentUISpecification.setFields(uiFields);
        result.setTopologyComponentUISpecification(componentUISpecification);
        return result;
    }

    private static ComponentUISpecification getCustomProcessorUISpecification (ComponentUISpecification componentUISpecification) {
        ComponentUISpecification result = new ComponentUISpecification();
        List<ComponentUISpecification.UIField> fields = new ArrayList<>();
        for (ComponentUISpecification.UIField uiField: componentUISpecification.getFields()) {
            if (uiField.getFieldName().startsWith(TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_PREFIX)) {
                ComponentUISpecification.UIField newUIField = new ComponentUISpecification.UIField(uiField);
                newUIField.setFieldName(uiField.getFieldName().replaceFirst(TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_PREFIX_REGEX, ""));
                fields.add(newUIField);
            }
        }
        result.setFields(fields);
        return (result.getFields().isEmpty() ? null : result);
    }

    private List<ComponentUISpecification.UIField> getCustomProcessorUIFieldsWithPrefix () {
        List<ComponentUISpecification.UIField> result = new ArrayList<>();
        if (topologyComponentUISpecification != null) {
            for (ComponentUISpecification.UIField uiField : this.topologyComponentUISpecification.getFields()) {
                ComponentUISpecification.UIField newUIField = new ComponentUISpecification.UIField(uiField);
                newUIField.setFieldName(TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_PREFIX + uiField.getFieldName());
                result.add(newUIField);
            }
        }
        return result;
    }


    private static Map<String, String> getPropertiesFromUIFields (List<ComponentUISpecification.UIField> uiFields) {
        Map<String, String> result = new HashMap<>();
        for (ComponentUISpecification.UIField uiField: uiFields) {
            if (PROPERTY_KEYS.contains(uiField.getFieldName())) {
                result.put(uiField.getFieldName(), (String) uiField.getDefaultValue());
            }
        }
        return result;
    }

    private ComponentUISpecification.UIField createUIField (String fieldName,
                                                            String uiName,
                                                            boolean isOptional,
                                                            boolean isUserInput,
                                                            String tooltip,
                                                            ComponentUISpecification.UIFieldType type,
                                                            Object defaultValue) {
        ComponentUISpecification.UIField uiField = new ComponentUISpecification.UIField();
        uiField.setFieldName(fieldName);
        uiField.setUiName(uiName);
        uiField.setIsOptional(isOptional);
        uiField.setIsUserInput(isUserInput);
        uiField.setTooltip(tooltip);
        uiField.setType(type);
        uiField.setDefaultValue(defaultValue);
        return uiField;
    }
}
