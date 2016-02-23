package com.hortonworks.iotas.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.topology.ConfigField;
import com.hortonworks.iotas.topology.TopologyComponent;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.topology.storm.CustomProcessorBoltFluxComponent;
import com.hortonworks.iotas.util.CoreUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class represting information about a custom processor helping in the registration process
 */
public class CustomProcessorInfo {
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String IMAGE_FILE_NAME = "imageFileName";
    public static final String JAR_FILE_NAME = "jarFileName";
    public static final String CONFIG_FIELDS = "configFields";
    public static final String INPUT_SCHEMA = "inputSchema";
    public static final String OUTPUT_STREAM_TO_SCHEMA = "outputStreamToSchema";
    public static final String CUSTOM_PROCESSOR_IMPL = "customProcessorImpl";

    private String streamingEngine;
    private String name;
    private String description;
    private String imageFileName;
    private String jarFileName;
    private List<ConfigField> configFields;
    private Schema inputSchema;
    private Map<String, Schema> outputStreamToSchema;
    private String customProcessorImpl;

    @Override
    public String toString() {
        return "CustomProcessorInfo{" +
                "streamingEngine='" + streamingEngine + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", imageFileName='" + imageFileName + '\'' +
                ", jarFileName='" + jarFileName + '\'' +
                ", configFields=" + configFields +
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
        if (imageFileName != null ? !imageFileName.equals(that.imageFileName) : that.imageFileName != null) return false;
        if (jarFileName != null ? !jarFileName.equals(that.jarFileName) : that.jarFileName != null) return false;
        if (configFields != null ? !configFields.equals(that.configFields) : that.configFields != null) return false;
        if (inputSchema != null ? !inputSchema.equals(that.inputSchema) : that.inputSchema != null) return false;
        if (outputStreamToSchema != null ? !outputStreamToSchema.equals(that.outputStreamToSchema) : that.outputStreamToSchema != null) return false;
        return !(customProcessorImpl != null ? !customProcessorImpl.equals(that.customProcessorImpl) : that.customProcessorImpl != null);

    }

    @Override
    public int hashCode() {
        int result = streamingEngine != null ? streamingEngine.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (imageFileName != null ? imageFileName.hashCode() : 0);
        result = 31 * result + (jarFileName != null ? jarFileName.hashCode() : 0);
        result = 31 * result + (configFields != null ? configFields.hashCode() : 0);
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

    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public String getJarFileName() {
        return jarFileName;
    }

    public void setJarFileName(String jarFileName) {
        this.jarFileName = jarFileName;
    }

    public List<ConfigField> getConfigFields() {
        return configFields;
    }

    public void setConfigFields(List<ConfigField> configFields) {
        this.configFields = configFields;
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

    public CustomProcessorInfo fromTopologyComponent (TopologyComponent topologyComponent) throws IOException {
        if (topologyComponent != null) {
            this.setStreamingEngine(topologyComponent.getStreamingEngine());
            List<ConfigField> configFields = this.getListOfConfigFields(topologyComponent);
            Map<String, Object> config = this.getProperties(configFields);
            this.setName((String) config.get(NAME));
            this.setDescription((String) config.get(DESCRIPTION));
            this.setJarFileName((String) config.get(JAR_FILE_NAME));
            this.setImageFileName((String) config.get(IMAGE_FILE_NAME));
            this.setCustomProcessorImpl((String) config.get(CUSTOM_PROCESSOR_IMPL));
            Schema inputSchema = CoreUtils.getSchemaFromConfig((Map) config.get(INPUT_SCHEMA));
            this.setInputSchema(inputSchema);
            Map<String, Schema> outputStreamToSchema = new HashMap<>();
            Map<String, Map> outputStreamToMap = (Map<String, Map>) config.get(OUTPUT_STREAM_TO_SCHEMA);
            for (Map.Entry<String, Map> entry: outputStreamToMap.entrySet()) {
                outputStreamToSchema.put(entry.getKey(), CoreUtils.getSchemaFromConfig(entry.getValue()));
            }
            this.setOutputStreamToSchema(outputStreamToSchema);
            this.setConfigFields(this.getCustomProcessorConfigFields(configFields));
        }
        return this;
    }

    public TopologyComponent toTopologyComponent () throws IOException {
        TopologyComponent result = new TopologyComponent();
        result.setTimestamp(System.currentTimeMillis());
        result.setType(TopologyComponent.TopologyComponentType.PROCESSOR);
        result.setSubType(TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_SUB_TYPE);
        result.setStreamingEngine(this.streamingEngine);
        if (TopologyLayoutConstants.STORM_STREAMING_ENGINE.equals(this.streamingEngine)) {
            result.setTransformationClass(CustomProcessorBoltFluxComponent.class.getCanonicalName());
            result.setName("customProcessorBoltComponent");
        }
        List<ConfigField> configFields = new ArrayList<>();
        configFields.addAll(this.getCustomProcessorConfigFieldsWithPrefix());
        configFields.add(this.createConfigField(TopologyLayoutConstants.JSON_KEY_PARALLELISM, true, true, TopologyLayoutConstants
                .JSON_KEY_PARALLELISM_TOOLTIP, ConfigField.Type.NUMBER, 1));
        configFields.add(this.createConfigField(TopologyLayoutConstants.JSON_KEY_LOCAL_JAR_PATH, false, true, TopologyLayoutConstants
                .JSON_KEY_LOCAL_JAR_PATH_TOOLTIP, ConfigField.Type.STRING, null));
        configFields.add(this.createConfigField(NAME, false, false, "Custom processor name", ConfigField.Type.STRING, this.name));
        configFields.add(this.createConfigField(DESCRIPTION, false, false, "Custom processor description", ConfigField.Type.STRING, this.description));
        configFields.add(this.createConfigField(IMAGE_FILE_NAME, false, false, "Custom processor image file", ConfigField.Type.STRING, this.imageFileName));
        configFields.add(this.createConfigField(JAR_FILE_NAME, false, false, "Custom processor jar file", ConfigField.Type.STRING, this.jarFileName));
        configFields.add(this.createConfigField(CUSTOM_PROCESSOR_IMPL, false, false, "Custom processor interface implementation class", ConfigField.Type
                .STRING, this.customProcessorImpl));
        configFields.add(this.createConfigField(INPUT_SCHEMA, false, false, "Custom processor input schema", ConfigField.Type.OBJECT, this.inputSchema));
        configFields.add(this.createConfigField(OUTPUT_STREAM_TO_SCHEMA, false, false, "Custom processor output schema", ConfigField.Type.OBJECT, this
                .outputStreamToSchema));
        ObjectMapper mapper = new ObjectMapper();
        result.setConfig(mapper.writeValueAsString(configFields));
        return result;
    }

    private List<ConfigField> getCustomProcessorConfigFields (List<ConfigField> configFields) {
        List<ConfigField> result = new ArrayList<>();
        for (ConfigField configField: configFields) {
            if (configField.getName().startsWith(TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_PREFIX)) {
                configField.setName(configField.getName().replaceFirst(TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_PREFIX_REGEX, ""));
                result.add(configField);
            }
        }
        return result;
    }

    private List<ConfigField> getCustomProcessorConfigFieldsWithPrefix () {
        List<ConfigField> result = new ArrayList<>();
        if (this.configFields != null && this.configFields.size() > 0) {
            for (ConfigField configField: this.configFields) {
                ConfigField newConfigField = new ConfigField();
                newConfigField.setName(TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_PREFIX + configField.getName());
                newConfigField.setIsOptional(configField.getIsOptional());
                newConfigField.setDefaultValue(configField.getDefaultValue());
                newConfigField.setIsUserInput(configField.getIsUserInput());
                newConfigField.setTooltip(configField.getTooltip());
                newConfigField.setType(configField.getType());
                result.add(newConfigField);
            }
        }
        return result;
    }

    private List<ConfigField> getListOfConfigFields (TopologyComponent topologyComponent) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<ConfigField> configFields = mapper.readValue(topologyComponent.getConfig(), new TypeReference<List<ConfigField>>() { });
        return configFields;
    }


    private Map<String, Object> getProperties (List<ConfigField> configFields) {
        Map<String, Object> result = new HashMap<String, Object>();
        Set<String> propertyKeys = this.getPropertyKeys();
        for (ConfigField configField: configFields) {
            if (propertyKeys.contains(configField.getName())) {
                result.put(configField.getName(), configField.getDefaultValue());
            }
        }
        return result;
    }

    private Set<String> getPropertyKeys () {
        Set<String> result = new HashSet<>();
        result.add(NAME);
        result.add(DESCRIPTION);
        result.add(IMAGE_FILE_NAME);
        result.add(JAR_FILE_NAME);
        result.add(CONFIG_FIELDS);
        result.add(INPUT_SCHEMA);
        result.add(OUTPUT_STREAM_TO_SCHEMA);
        result.add(CUSTOM_PROCESSOR_IMPL);
        return result;
    }

   private ConfigField createConfigField (String name, boolean isOptional, boolean isUserInput, String tooltip, ConfigField.Type type, Object defaultValue) {
        ConfigField configField = new ConfigField();
        configField.setName(name);
        configField.setIsOptional(isOptional);
        configField.setIsUserInput(isUserInput);
        configField.setTooltip(tooltip);
        configField.setType(type);
        configField.setDefaultValue(defaultValue);
        return configField;
    }
}
