package com.hortonworks.iotas.topology.storm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.hortonworks.iotas.catalog.Topology;
import com.hortonworks.iotas.topology.StatusImpl;
import com.hortonworks.iotas.topology.TopologyActions;
import com.hortonworks.iotas.topology.TopologyLayoutConstants;
import com.hortonworks.iotas.util.ReflectionHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Storm implementation of the TopologyActions interface
 */
public class StormTopologyActionsImpl implements TopologyActions {
    private static final Logger LOG = LoggerFactory.getLogger(StormTopologyActionsImpl.class);

    private String stormArtifactsLocation = "/tmp/storm-artifacts/";
    private String stormCliPath = "storm";
    private String stormJarLocation;
    private String catalogRootUrl;
    private String javaJarCommand;

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
                String stormHomeDir = conf.get(TopologyLayoutConstants.STORM_HOME_DIR).toString();
                if (!stormHomeDir.endsWith(File.separator)) {
                    stormHomeDir += File.separator;
                }
                stormCliPath = stormHomeDir + "bin" + File.separator + "storm";
            }
            stormJarLocation = conf.get(TopologyLayoutConstants.STORM_JAR_LOCATION_KEY);
            catalogRootUrl = conf.get(TopologyLayoutConstants.YAML_KEY_CATALOG_ROOT_URL);
            String res;
            if ((res=conf.get(TopologyLayoutConstants.JAVA_JAR_COMMAND)) != null) {
                javaJarCommand = res;
            } else {
                javaJarCommand = "jar";
            }
        }
        File f = new File (stormArtifactsLocation);
        f.mkdirs();
    }


    @Override
    public void deploy (Topology topology) throws Exception {
        Path jarToDeploy = addArtifactsToJar(getArtifactsLocation(topology));
        String fileName = this.createYamlFile(topology);
        List<String> commands = new ArrayList<String>();
        commands.add(stormCliPath);
        commands.add("jar");
        commands.add(jarToDeploy.toString());
        commands.add("org.apache.storm.flux.Flux");
        commands.add("--remote");
        commands.add(fileName);
        int exitValue = executeShellProcess(commands).exitValue;
        if (exitValue != 0) {
            throw new Exception("Topology could not be deployed " +
                    "successfully.");
        }
    }

    private Path addArtifactsToJar(Path artifactsLocation) throws Exception {
        Path jarFile = Paths.get(stormJarLocation);
        if (artifactsLocation.toFile().isDirectory()) {
            File[] artifacts = artifactsLocation.toFile().listFiles();
            if (artifacts.length > 0) {
                Path newJar = Files.copy(jarFile, artifactsLocation.resolve(jarFile.getFileName()));
                for (File artifact : artifacts) {
                    executeShellProcess(
                            ImmutableList.of(javaJarCommand, "uf", newJar.toString(),
                                             "-C", artifactsLocation.toString(), artifact.getName()));
                    LOG.debug("Added file {} to jar {}", artifact, jarFile);
                }
                return newJar;
            }
        } else {
            LOG.debug("Artifacts directory {} does not exist, not adding any artifacts to jar", artifactsLocation);
        }
        return jarFile;
    }

    @Override
    public void kill (Topology topology) throws Exception {
        List<String> commands = new ArrayList<String>();
        commands.add(stormCliPath);
        commands.add("kill");
        commands.add(getTopologyName(topology));
        int exitValue = executeShellProcess(commands).exitValue;
        if (exitValue != 0) {
            throw new Exception("Topology could not be killed " +
                    "successfully.");
        }
        File artifactsDir = getArtifactsLocation(topology).toFile();
        if (artifactsDir.exists() && artifactsDir.isDirectory()) {
            LOG.debug("Cleaning up {}", artifactsDir);
            FileUtils.cleanDirectory(artifactsDir);
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
        int exitValue = executeShellProcess(commands).exitValue;
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
        int exitValue = executeShellProcess(commands).exitValue;
        if (exitValue != 0) {
            throw new Exception("Topology could not be resumed " +
                    "successfully.");
        }
    }

    @Override
    public Status status(Topology topology) throws Exception {
        StatusImpl status = new StatusImpl();
        List<String> commands = new ArrayList<String>();
        commands.add(stormCliPath);
        commands.add("list");
        String topologyName = getTopologyName(topology);
        ShellProcessResult result = executeShellProcess(commands);
        int exitValue = result.exitValue;
        if (exitValue != 0) {
            LOG.error("Topology status could not be fetched for {}", topology.getName());
        } else {
            BufferedReader reader = new BufferedReader(new StringReader(result.stdout));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\s+");
                if (fields[0].equals(topologyName)) {
                    status.setStatus(fields[1]);
                    status.putExtra("Num_tasks", fields[2]);
                    status.putExtra("Num_workers", fields[3]);
                    status.putExtra("Uptime_secs", fields[4]);
                    break;
                }
            }
        }
        return status;
    }

    /**
     * the Path where any topology specific artifacts are kept
     */
    @Override
    public Path getArtifactsLocation(Topology topology) {
        return Paths.get(stormArtifactsLocation, getTopologyName(topology));
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


    private ShellProcessResult executeShellProcess (List<String> commands) throws  Exception {
        LOG.debug("Executing command: {}", Joiner.on(" ").join(commands));
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        StringWriter sw = new StringWriter();
        IOUtils.copy(process.getInputStream(), sw);
        String stdout = sw.toString();
        process.waitFor();
        int exitValue = process.exitValue();
        LOG.debug("Command output: {}", stdout);
        LOG.debug("Command exit status: {}", exitValue);
        return new ShellProcessResult(exitValue, stdout);
    }

    private static class ShellProcessResult {
        private final int exitValue;
        private final String stdout;
        ShellProcessResult(int exitValue, String stdout) {
            this.exitValue = exitValue;
            this.stdout = stdout;
        }
    }
}
