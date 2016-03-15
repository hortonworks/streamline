package com.hortonworks.iotas.topology.storm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.catalog.Topology;
import com.hortonworks.iotas.topology.TopologyActions;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.util.ReflectionHelper;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Storm implementation of the TopologyActions interface
 */
public class StormTopologyActionsImpl implements TopologyActions {

    private String stormArtifactsLocation = "/tmp/storm-artifacts/";
    private String stormCliPath = "storm";
    private String stormJarLocation;
    private String catalogRootUrl;

    private Map<String, String> jsonUiNameToStormName = new HashMap<>();

    public StormTopologyActionsImpl() {
    }

    @Override
    public void init (Map<String, String> conf) {
        if (conf != null) {
            if (conf.containsKey(TopologyLayoutConstants.STORM_ARTIFACTS_LOCATION_KEY)) {
                stormArtifactsLocation = conf.get(TopologyLayoutConstants.STORM_ARTIFACTS_LOCATION_KEY);
            }
            if (conf.containsKey(TopologyLayoutConstants.STORM_HOME_DIR)) {
                stormCliPath = conf.get(TopologyLayoutConstants.STORM_HOME_DIR) + "bin/storm";
            }
            stormJarLocation = conf.get(TopologyLayoutConstants.STORM_JAR_LOCATION_KEY);
            catalogRootUrl = conf.get(TopologyLayoutConstants.YAML_KEY_CATALOG_ROOT_URL);
        }
        File f = new File (stormArtifactsLocation);
        f.mkdirs();
    }

    @Override
    public void deploy (Topology topology) throws Exception {
        String fileName = this.createYamlFile(topology);
        List<String> commands = new ArrayList<String>();
        commands.add(stormCliPath);
        commands.add("jar");
        commands.add(stormJarLocation);
        commands.add("org.apache.storm.flux.Flux");
        commands.add("--remote");
        commands.add(fileName);
        int exitValue = executeShellProcess(commands);
        if (exitValue != 0) {
            throw new Exception("Topology could not be deployed " +
                    "successfully.");
        }
    }

    @Override
    public void kill (Topology topology) throws Exception {
        List<String> commands = new ArrayList<String>();
        commands.add(stormCliPath);
        commands.add("kill");
        commands.add(getTopologyName(topology));
        int exitValue = executeShellProcess(commands);
        if (exitValue != 0) {
            throw new Exception("Topology could not be killed " +
                    "successfully.");
        }
    }

    @Override
    public void validate (Topology topology) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map topologyConfig = mapper.readValue(topology.getConfig(), Map.class);
        StormTopologyValidator validator = new StormTopologyValidator(topologyConfig, this.catalogRootUrl);
        validator.validate();
    }

    @Override
    public void suspend (Topology topology) throws Exception {
        List<String> commands = new ArrayList<String>();
        commands.add(stormCliPath);
        commands.add("deactivate");
        commands.add(getTopologyName(topology));
        int exitValue = executeShellProcess(commands);
        if (exitValue != 0) {
            throw new Exception("Topology could not be suspended " +
                    "successfully.");
        }
    }

    @Override
    public void resume (Topology topology) throws Exception {
        List<String> commands = new ArrayList<String>();
        commands.add(stormCliPath);
        commands.add("activate");
        commands.add(getTopologyName(topology));
        int exitValue = executeShellProcess(commands);
        if (exitValue != 0) {
            throw new Exception("Topology could not be resumed " +
                    "successfully.");
        }
    }

    private String createYamlFile (Topology topology) throws
            Exception {
        String configJson = topology.getConfig();
        Map<String, Object> yamlMap;
        Map jsonMap;
        ObjectMapper objectMapper = new ObjectMapper();
        File f;
        FileWriter fileWriter = null;
        try {
            f = new File(this.getFilePath(topology));
            if (f.exists()) {
                if (!f.delete()) {
                    throw new Exception("Unable to delete old storm " +
                            "artifact for topology id " + topology.getId());
                }
            }

            jsonMap = objectMapper.readValue(configJson, Map.class);
            yamlMap = new LinkedHashMap<String, Object>();
            yamlMap.put(TopologyLayoutConstants.YAML_KEY_NAME, this
                    .getTopologyName(topology));
            addTopologyConfig(yamlMap, (Map<String, Object>) jsonMap.get
                    (TopologyLayoutConstants.JSON_KEY_CONFIG));
            addToYamlTopLevelComponents(yamlMap, (List<Map<String, Object>>) jsonMap.get(TopologyLayoutConstants.JSON_KEY_DATA_SOURCES), TopologyLayoutConstants.YAML_KEY_SPOUTS);
            addToYamlTopLevelComponents(yamlMap, (List<Map<String, Object>>) jsonMap.get(TopologyLayoutConstants.JSON_KEY_PROCESSORS), TopologyLayoutConstants.YAML_KEY_BOLTS);
            addToYamlTopLevelComponents(yamlMap, (List<Map<String, Object>>) jsonMap.get(TopologyLayoutConstants.JSON_KEY_DATA_SINKS), TopologyLayoutConstants.YAML_KEY_BOLTS);
            addToYamlTopLevelComponents(yamlMap, (List<Map<String, Object>>) jsonMap.get(TopologyLayoutConstants.JSON_KEY_LINKS), TopologyLayoutConstants.YAML_KEY_STREAMS);
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            //options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
            Yaml yaml = new Yaml (options);
            fileWriter = new FileWriter(f);
            yaml.dump(yamlMap, fileWriter);
            return f.getAbsolutePath();
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }

    private String getTopologyName (Topology topology) {
        return "iotas-" + topology.getId() + "-" + topology.getName();
    }

    private String getFilePath (Topology topology) {
        return this.stormArtifactsLocation + getTopologyName(topology) + "" +
                ".yaml";
    }

    // Add topology level configs. catalogRootUrl, hbaseConf, hdfsConf,
    // numWorkers, etc.
    private void addTopologyConfig (Map<String, Object> yamlMap, Map<String,
            Object> topologyConfig) {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put(TopologyLayoutConstants.YAML_KEY_CATALOG_ROOT_URL, catalogRootUrl);
        config.putAll(topologyConfig);
        yamlMap.put(TopologyLayoutConstants.YAML_KEY_CONFIG, config);
    }

    private void addToYamlTopLevelComponents (Map<String, Object> yamlMap, List<Map<String,
            Object>> components, String collectionKey) throws Exception {
        for (Map component: components) {
            String transformationClass = (String) component.get
                    (TopologyLayoutConstants.JSON_KEY_TRANSFORMATION_CLASS);
            String uiname = (String) component.get
                    (TopologyLayoutConstants.JSON_KEY_UINAME);
            Map<String, Object> config = (Map<String, Object>) component.get
                    (TopologyLayoutConstants.JSON_KEY_CONFIG);
            FluxComponent fluxComponent = (FluxComponent) ReflectionHelper
                    .newInstance(transformationClass);
            fluxComponent.withConfig(config);
            List<Map<String, Object>> referencedComponents = fluxComponent
                    .getReferencedComponents();
            // add all the components referenced by this yaml component
            for (Map<String, Object> referencedComponent: referencedComponents) {
                this.addComponentToCollection(yamlMap, referencedComponent,
                        TopologyLayoutConstants.YAML_KEY_COMPONENTS);
            }
            Map<String, Object> yamlComponent = fluxComponent
                    .getComponent();
            // getComponent either returns a spouts/bolt or stream. Update
            // the id or the name field to uiname to guarantee uniqueness in
            // yaml file since uiname is presumed to be unique
            String idField = yamlComponent.containsKey
                    (TopologyLayoutConstants.YAML_KEY_ID) ? TopologyLayoutConstants.YAML_KEY_ID : TopologyLayoutConstants.YAML_KEY_NAME;
            yamlComponent.put(idField, uiname);
            // add the yaml component itself
            this.addComponentToCollection(yamlMap, yamlComponent, collectionKey);
        }
        return;
    }


    private void addComponentToCollection (Map<String, Object> yamlMap, Map<String, Object> yamlComponent, String collectionKey) {
        if (yamlComponent == null ) {
            return;
        }
        List<Map<String, Object>> components = (ArrayList) yamlMap.get
                (collectionKey);
        if (components == null) {
            components = new ArrayList<Map<String, Object>>();
            yamlMap.put(collectionKey, components);
        }
        components.add(yamlComponent);
    }


    private int executeShellProcess (List<String> commands) throws  Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.inheritIO();
        Process process = processBuilder.start();
        process.waitFor();
        int exitValue = process.exitValue();
        System.out.println("Exit value from subprocess is :" + exitValue);
        return exitValue;
    }

}
