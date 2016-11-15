package org.apache.streamline.streams.catalog.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.common.Schema;
import org.apache.streamline.common.util.Utils;
import org.apache.streamline.streams.catalog.topology.TopologyComponentBundle;
import org.apache.streamline.streams.catalog.topology.TopologyComponentUISpecification;
import org.apache.streamline.streams.layout.TopologyLayoutConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class representing information about a custom processor helping in the registration process
 */
public class CustomProcessorInfo {
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String JAR_FILE_NAME = "jarFileName";
    public static final String INPUT_SCHEMA = "inputSchema";
    public static final String OUTPUT_STREAM_TO_SCHEMA = "outputStreamToSchema";
    public static final String CUSTOM_PROCESSOR_IMPL = "customProcessorImpl";

    private String streamingEngine;
    private String name;
    private String description;
    private String jarFileName;
    private TopologyComponentUISpecification topologyComponentUISpecification;
    private Schema inputSchema;
    private Map<String, Schema> outputStreamToSchema;
    private String customProcessorImpl;

    @Override
    public String toString() {
        return "CustomProcessorInfo{" +
                "streamingEngine='" + streamingEngine + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", jarFileName='" + jarFileName + '\'' +
                ", topologyComponentUISpecification='" + topologyComponentUISpecification+ '\'' +
                ", inputSchema=" + inputSchema +
                ", outputStreamToSchema=" + outputStreamToSchema +
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
        if (outputStreamToSchema != null ? !outputStreamToSchema.equals(that.outputStreamToSchema) : that.outputStreamToSchema != null) return false;
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
        result = 31 * result + (outputStreamToSchema != null ? outputStreamToSchema.hashCode() : 0);
        result = 31 * result + (customProcessorImpl != null ? customProcessorImpl.hashCode() : 0);
        return result;
    }

    public Map<String, Schema> getOutputStreamToSchema() {
        return outputStreamToSchema;
    }

    public void setOutputStreamToSchema(Map<String, Schema> outputStreamToSchema) {
        this.outputStreamToSchema = outputStreamToSchema;
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

    public String getJarFileName() {
        return jarFileName;
    }

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

    public TopologyComponentUISpecification getTopologyComponentUISpecification() {
        return topologyComponentUISpecification;
    }

    public void setTopologyComponentUISpecification(TopologyComponentUISpecification topologyComponentUISpecification) {
        this.topologyComponentUISpecification = topologyComponentUISpecification;
    }

    public CustomProcessorInfo fromTopologyComponentBundle (TopologyComponentBundle topologyComponentBundle) throws IOException {
        if (topologyComponentBundle != null) {
            this.setStreamingEngine(topologyComponentBundle.getStreamingEngine());
            TopologyComponentUISpecification topologyComponentUISpecification = topologyComponentBundle.getTopologyComponentUISpecification();
            List<TopologyComponentUISpecification.UIField> uiFields = topologyComponentUISpecification.getFields();
            Map<String, String> config = this.getPropertiesFromUIFields(uiFields);
            this.setName(config.get(NAME));
            this.setDescription(config.get(DESCRIPTION));
            this.setJarFileName(config.get(JAR_FILE_NAME));
            this.setCustomProcessorImpl(config.get(CUSTOM_PROCESSOR_IMPL));
            this.setInputSchema(Utils.getSchemaFromConfig(config.get(INPUT_SCHEMA)));
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Schema> outputStreamToSchema = objectMapper.readValue(config.get(OUTPUT_STREAM_TO_SCHEMA), new TypeReference<Map<String, Schema>>() {});
            this.setOutputStreamToSchema(outputStreamToSchema);
            this.setTopologyComponentUISpecification(getCustomProcessorUISpecification(topologyComponentUISpecification));
        }
        return this;
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
        result.setTransformationClass("org.apache.streamline.streams.layout.storm.CustomProcessorBoltFluxComponent");
        List<TopologyComponentUISpecification.UIField> uiFields = new ArrayList<>();
        uiFields.addAll(getCustomProcessorUIFieldsWithPrefix());
        uiFields.add(this.createUIField(TopologyLayoutConstants.JSON_KEY_PARALLELISM, TopologyLayoutConstants.JSON_KEY_PARALLELISM, true, true,
                TopologyLayoutConstants.JSON_KEY_PARALLELISM_TOOLTIP, TopologyComponentUISpecification.UIFieldType.NUMBER.NUMBER, 1));
        uiFields.add(this.createUIField(TopologyLayoutConstants.JSON_KEY_LOCAL_JAR_PATH, TopologyLayoutConstants.JSON_KEY_LOCAL_JAR_PATH, false, true,
                TopologyLayoutConstants.JSON_KEY_LOCAL_JAR_PATH_TOOLTIP, TopologyComponentUISpecification.UIFieldType.STRING, null));
        uiFields.add(this.createUIField(NAME, NAME, false, false, "Custom processor name", TopologyComponentUISpecification.UIFieldType.STRING, this.name));
        uiFields.add(this.createUIField(DESCRIPTION, DESCRIPTION, false, false, "Custom processor description", TopologyComponentUISpecification.UIFieldType
                .STRING, this.description));
        uiFields.add(this.createUIField(JAR_FILE_NAME, JAR_FILE_NAME, false, false, "Custom processor jar file", TopologyComponentUISpecification.UIFieldType
                .STRING, this.jarFileName));
        uiFields.add(this.createUIField(CUSTOM_PROCESSOR_IMPL, CUSTOM_PROCESSOR_IMPL, false, false, "Custom processor interface implementation class",
                TopologyComponentUISpecification.UIFieldType.STRING, this.customProcessorImpl));
        ObjectMapper objectMapper = new ObjectMapper();
        uiFields.add(this.createUIField(INPUT_SCHEMA, INPUT_SCHEMA, false, false, "Custom processor input schema", TopologyComponentUISpecification
                .UIFieldType.STRING, objectMapper.writeValueAsString(this.inputSchema)));
        uiFields.add(this.createUIField(OUTPUT_STREAM_TO_SCHEMA, OUTPUT_STREAM_TO_SCHEMA, false, false, "Custom processor output schema",
                TopologyComponentUISpecification.UIFieldType.STRING, objectMapper.writeValueAsString(this.outputStreamToSchema)));
        TopologyComponentUISpecification topologyComponentUISpecification = new TopologyComponentUISpecification();
        topologyComponentUISpecification.setFields(uiFields);
        result.setTopologyComponentUISpecification(topologyComponentUISpecification);
        return result;
    }

    private TopologyComponentUISpecification getCustomProcessorUISpecification (TopologyComponentUISpecification topologyComponentUISpecification) {
        TopologyComponentUISpecification result = new TopologyComponentUISpecification();
        List<TopologyComponentUISpecification.UIField> fields = new ArrayList<>();
        for (TopologyComponentUISpecification.UIField uiField: topologyComponentUISpecification.getFields()) {
            if (uiField.getFieldName().startsWith(TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_PREFIX)) {
                TopologyComponentUISpecification.UIField newUIField = new TopologyComponentUISpecification.UIField(uiField);
                newUIField.setFieldName(uiField.getFieldName().replaceFirst(TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_PREFIX_REGEX, ""));
                fields.add(newUIField);
            }
        }
        result.setFields(fields);
        return result;
    }

    private List<TopologyComponentUISpecification.UIField> getCustomProcessorUIFieldsWithPrefix () {
        List<TopologyComponentUISpecification.UIField> result = new ArrayList<>();
        for (TopologyComponentUISpecification.UIField uiField: this.topologyComponentUISpecification.getFields()) {
            TopologyComponentUISpecification.UIField newUIField = new TopologyComponentUISpecification.UIField(uiField);
            newUIField.setFieldName(TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_PREFIX + uiField.getFieldName());
            result.add(newUIField);
        }
        return result;
    }


    private Map<String, String> getPropertiesFromUIFields (List<TopologyComponentUISpecification.UIField> uiFields) {
        Map<String, String> result = new HashMap<>();
        Set<String> propertyKeys = this.getPropertyKeys();
        for (TopologyComponentUISpecification.UIField uiField: uiFields) {
            if (propertyKeys.contains(uiField.getFieldName())) {
                result.put(uiField.getFieldName(), (String) uiField.getDefaultValue());
            }
        }
        return result;
    }

    private Set<String> getPropertyKeys () {
        Set<String> result = new HashSet<>();
        result.add(NAME);
        result.add(DESCRIPTION);
        result.add(JAR_FILE_NAME);
        result.add(INPUT_SCHEMA);
        result.add(OUTPUT_STREAM_TO_SCHEMA);
        result.add(CUSTOM_PROCESSOR_IMPL);
        return result;
    }

    private TopologyComponentUISpecification.UIField createUIField (String fieldName, String uiName, boolean isOptional, boolean isUserInput, String tooltip,
                                                                    TopologyComponentUISpecification.UIFieldType type, Object defaultValue) {
        TopologyComponentUISpecification.UIField uiField = new TopologyComponentUISpecification.UIField();
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
