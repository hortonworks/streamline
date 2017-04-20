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
package com.hortonworks.streamline.streams.actions.storm.topology;

import com.google.common.base.Joiner;
import com.hortonworks.streamline.common.exception.service.exception.request.TopologyAlreadyExistsOnCluster;
import com.hortonworks.streamline.streams.actions.TopologyActionContext;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSource;
import com.hortonworks.streamline.streams.storm.common.StormJaasCreator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.streams.actions.StatusImpl;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.layout.storm.StormTopologyFluxGenerator;
import com.hortonworks.streamline.streams.layout.storm.StormTopologyLayoutConstants;
import com.hortonworks.streamline.streams.layout.storm.StormTopologyValidator;
import com.hortonworks.streamline.streams.storm.common.StormRestAPIClient;
import com.hortonworks.streamline.streams.storm.common.StormTopologyUtil;
import com.hortonworks.streamline.streams.exception.TopologyNotAliveException;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Storm implementation of the TopologyActions interface
 */
public class StormTopologyActionsImpl implements TopologyActions {
    private static final Logger LOG = LoggerFactory.getLogger(StormTopologyActionsImpl.class);
    public static final int DEFAULT_WAIT_TIME_SEC = 30;
    public static final int TEST_RUN_TOPOLOGY_WAIT_MILLIS_FOR_SHUTDOWN = 30_000;

    private static final String DEFAULT_THRIFT_TRANSPORT_PLUGIN = "org.apache.storm.security.auth.SimpleTransportPlugin";
    private static final String DEFAULT_PRINCIPAL_TO_LOCAL = "org.apache.storm.security.auth.DefaultPrincipalToLocal";

    private static final String NIMBUS_SEEDS = "nimbus.seeds";
    private static final String NIMBUS_PORT = "nimbus.port";

    private String stormArtifactsLocation = "/tmp/storm-artifacts/";
    private String stormCliPath = "storm";
    private String stormJarLocation;
    private String catalogRootUrl;
    private String javaJarCommand;
    private StormRestAPIClient client;
    private String nimbusSeeds;
    private Integer nimbusPort;
    private Map<String, String> conf;

    private String thriftTransport;
    private Optional<String> jaasFilePath;
    private String principalToLocal;

    public StormTopologyActionsImpl() {
    }

    @Override
    public void init (Map<String, String> conf) {
        this.conf = conf;
        if (conf != null) {
            if (conf.containsKey(StormTopologyLayoutConstants.STORM_ARTIFACTS_LOCATION_KEY)) {
                stormArtifactsLocation = conf.get(StormTopologyLayoutConstants.STORM_ARTIFACTS_LOCATION_KEY);
            }
            if (conf.containsKey(StormTopologyLayoutConstants.STORM_HOME_DIR)) {
                String stormHomeDir = conf.get(StormTopologyLayoutConstants.STORM_HOME_DIR);
                if (!stormHomeDir.endsWith(File.separator)) {
                    stormHomeDir += File.separator;
                }
                stormCliPath = stormHomeDir + "bin" + File.separator + "storm";
            }
            this.stormJarLocation = conf.get(StormTopologyLayoutConstants.STORM_JAR_LOCATION_KEY);

            catalogRootUrl = conf.get(StormTopologyLayoutConstants.YAML_KEY_CATALOG_ROOT_URL);

            Map<String, String> env = System.getenv();
            String javaHomeStr = env.get("JAVA_HOME");
            if (StringUtils.isNotEmpty(javaHomeStr)) {
                if (!javaHomeStr.endsWith(File.separator)) {
                    javaHomeStr += File.separator;
                }
                javaJarCommand = javaHomeStr + "bin" + File.separator + "jar";
            } else {
                javaJarCommand = "jar";
            }

            String stormApiRootUrl = null;
            if (conf != null) {
                stormApiRootUrl = conf.get(TopologyLayoutConstants.STORM_API_ROOT_URL_KEY);
            }
            Client restClient = ClientBuilder.newClient(new ClientConfig());
            this.client = new StormRestAPIClient(restClient, stormApiRootUrl);
            nimbusSeeds = conf.get(NIMBUS_SEEDS);
            nimbusPort = Integer.valueOf(conf.get(NIMBUS_PORT));
        }
        File f = new File (stormArtifactsLocation);
        f.mkdirs();

        setupSecuredStormCluster(conf);
    }

    private void setupSecuredStormCluster(Map<String, String> conf) {
        thriftTransport = conf.get(TopologyLayoutConstants.STORM_THRIFT_TRANSPORT);

        if (conf.containsKey(TopologyLayoutConstants.STORM_NIMBUS_PRINCIPAL_NAME)) {
            String nimbusPrincipal = conf.get(TopologyLayoutConstants.STORM_NIMBUS_PRINCIPAL_NAME);
            String kerberizedNimbusServiceName = nimbusPrincipal.split("/")[0];
            jaasFilePath = Optional.of(createJaasFile(kerberizedNimbusServiceName));
        } else {
            jaasFilePath = Optional.empty();
        }

        principalToLocal = conf.getOrDefault(TopologyLayoutConstants.STORM_PRINCIPAL_TO_LOCAL, DEFAULT_PRINCIPAL_TO_LOCAL);

        if (thriftTransport == null) {
            if (jaasFilePath.isPresent()) {
                thriftTransport = conf.get(TopologyLayoutConstants.STORM_SECURED_THRIFT_TRANSPORT);
            } else {
                thriftTransport = conf.get(TopologyLayoutConstants.STORM_NONSECURED_THRIFT_TRANSPORT);
            }
        }

        // if it's still null, set to default
        if (thriftTransport == null) {
            thriftTransport = DEFAULT_THRIFT_TRANSPORT_PLUGIN;
        }
    }

    @Override
    public void deploy(TopologyLayout topology, String mavenArtifacts, TopologyActionContext ctx, String asUser) throws Exception {
        ctx.setCurrentAction("Adding artifacts to jar");
        Path jarToDeploy = addArtifactsToJar(getArtifactsLocation(topology));
        ctx.setCurrentAction("Creating Storm topology YAML file");
        String fileName = createYamlFile(topology);
        ctx.setCurrentAction("Deploying topology via 'storm jar' command");
        List<String> commands = new ArrayList<String>();
        commands.add(stormCliPath);
        commands.add("jar");
        commands.add(jarToDeploy.toString());
        commands.addAll(getExtraJarsArg(topology));
        commands.addAll(getMavenArtifactsRelatedArgs(mavenArtifacts));
        commands.addAll(getNimbusConf());
        commands.addAll(getSecuredClusterConf(asUser));
        commands.add("org.apache.storm.flux.Flux");
        commands.add("--remote");
        commands.add(fileName);
        ShellProcessResult shellProcessResult = executeShellProcess(commands);
        int exitValue = shellProcessResult.exitValue;
        if (exitValue != 0) {
            LOG.error("Topology deploy command failed - exit code: {} / output: {}", exitValue, shellProcessResult.stdout);
            String[] lines = shellProcessResult.stdout.split("\\n");
            String errors = Arrays.stream(lines)
                    .filter(line -> line.startsWith("Exception"))
                    .collect(Collectors.joining(", "));
            Pattern pattern = Pattern.compile("Topology with name `(.*)` already exists on cluster");
            Matcher matcher = pattern.matcher(errors);
            if (matcher.find()) {
                throw new TopologyAlreadyExistsOnCluster(matcher.group(1));
            } else {
                throw new Exception("Topology could not be deployed successfully: storm deploy command failed with " + errors);
            }
        }
    }

    @Override
    public void testRun(TopologyLayout topology, String mavenArtifacts,
                        Map<String, TestRunSource> testRunSourcesForEachSource,
                        Map<String, TestRunProcessor> testRunProcessorsForEachProcessor,
                        Map<String, TestRunSink> testRunSinksForEachSink) throws Exception {
        TopologyDag originalTopologyDag = topology.getTopologyDag();

        TestTopologyDagCreatingVisitor visitor = new TestTopologyDagCreatingVisitor(originalTopologyDag,
                testRunSourcesForEachSource, testRunProcessorsForEachProcessor, testRunSinksForEachSink);
        originalTopologyDag.traverse(visitor);
        TopologyDag testTopologyDag = visitor.getTestTopologyDag();

        TopologyLayout testTopology = copyTopologyLayout(topology, testTopologyDag);

        Path jarToDeploy = addArtifactsToJar(getArtifactsLocation(testTopology));
        String fileName = createYamlFile(testTopology);
        List<String> commands = new ArrayList<String>();
        commands.add(stormCliPath);
        commands.add("jar");
        commands.add(jarToDeploy.toString());
        commands.addAll(getExtraJarsArg(testTopology));
        commands.addAll(getMavenArtifactsRelatedArgs(mavenArtifacts));
        commands.addAll(getNimbusConf());
        commands.add("org.apache.storm.flux.Flux");
        commands.add("--local");
        commands.add("-s");
        commands.add(String.valueOf(TEST_RUN_TOPOLOGY_WAIT_MILLIS_FOR_SHUTDOWN));
        commands.add(fileName);

        ShellProcessResult shellProcessResult = executeShellProcess(commands);
        int exitValue = shellProcessResult.exitValue;
        if (exitValue != 0) {
            LOG.error("Topology deploy command as test mode failed - exit code: {} / output: {}", exitValue, shellProcessResult.stdout);
            throw new Exception("Topology could not be run " +
                    "successfully as test mode: storm deploy command failed");
        }
    }

    @Override
    public void kill (TopologyLayout topology, String asUser) throws Exception {
        String stormTopologyId = getRuntimeTopologyId(topology, asUser);

        boolean killed = client.killTopology(stormTopologyId, asUser, DEFAULT_WAIT_TIME_SEC);
        if (!killed) {
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
    public void validate (TopologyLayout topology) throws Exception {
        Map<String, Object> topologyConfig = topology.getConfig().getProperties();
        if (!topologyConfig.isEmpty()) {
            StormTopologyValidator validator = new StormTopologyValidator(topologyConfig, this.catalogRootUrl);
            validator.validate();
        }
    }

    @Override
    public void suspend (TopologyLayout topology, String asUser) throws Exception {
        String stormTopologyId = getRuntimeTopologyId(topology, asUser);

        boolean suspended = client.deactivateTopology(stormTopologyId, asUser);
        if (!suspended) {
            throw new Exception("Topology could not be suspended " +
                    "successfully.");
        }
    }

    @Override
    public void resume (TopologyLayout topology, String asUser) throws Exception {
        String stormTopologyId = getRuntimeTopologyId(topology, asUser);
        if (stormTopologyId == null || stormTopologyId.isEmpty()) {
            throw new TopologyNotAliveException("Topology not found in Storm Cluster - topology id: " + topology.getId());
        }

        boolean resumed = client.activateTopology(stormTopologyId, asUser);
        if (!resumed) {
            throw new Exception("Topology could not be resumed " +
                    "successfully.");
        }
    }

    @Override
    public Status status(TopologyLayout topology, String asUser) throws Exception {
        String stormTopologyId = getRuntimeTopologyId(topology, asUser);

        Map topologyStatus = client.getTopology(stormTopologyId, asUser);

        StatusImpl status = new StatusImpl();
        status.setStatus((String) topologyStatus.get("status"));
        status.putExtra("Num_tasks", String.valueOf(topologyStatus.get("workersTotal")));
        status.putExtra("Num_workers", String.valueOf(topologyStatus.get("tasksTotal")));
        status.putExtra("Uptime_secs", String.valueOf(topologyStatus.get("uptimeSeconds")));
        return status;
    }

    /**
     * the Path where any topology specific artifacts are kept
     */
    @Override
    public Path getArtifactsLocation(TopologyLayout topology) {
        return Paths.get(stormArtifactsLocation, generateStormTopologyName(topology), "artifacts");
    }

    @Override
    public Path getExtraJarsLocation(TopologyLayout topology) {
        return Paths.get(stormArtifactsLocation, generateStormTopologyName(topology), "jars");
    }

    @Override
    public String getRuntimeTopologyId(TopologyLayout topology, String asUser) {
        String stormTopologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        if (StringUtils.isEmpty(stormTopologyId)) {
            throw new TopologyNotAliveException("Topology not found in Storm Cluster - topology id: " + topology.getId());
        }
        return stormTopologyId;
    }

    private TopologyLayout copyTopologyLayout(TopologyLayout topology, TopologyDag replacedTopologyDag) {
        return new TopologyLayout(topology.getId(), topology.getName(), topology.getConfig(), replacedTopologyDag);
    }

    private List<String> getMavenArtifactsRelatedArgs (String mavenArtifacts) {
        List<String> args = new ArrayList<>();
        if (mavenArtifacts != null && !mavenArtifacts.isEmpty()) {
            args.add("--artifacts");
            args.add(mavenArtifacts);
            args.add("--artifactRepositories");
            args.add(conf.get("mavenRepoUrl"));
        }
        return args;
    }

    private List<String> getExtraJarsArg(TopologyLayout topology) {
        List<String> args = new ArrayList<>();
        List<String> jars = new ArrayList<>();
        Path extraJarsPath = getExtraJarsLocation(topology);
        if (extraJarsPath.toFile().isDirectory()) {
            File[] jarFiles = extraJarsPath.toFile().listFiles();
            if (jarFiles != null && jarFiles.length > 0) {
                for (File jarFile : jarFiles) {
                    jars.add(jarFile.getAbsolutePath());
                }
            }
        } else {
            LOG.debug("Extra jars directory {} does not exist, not adding any extra jars", extraJarsPath);
        }
        if (!jars.isEmpty()) {
            args.add("--jars");
            args.add(Joiner.on(",").join(jars));
        }
        return args;
    }

    private List<String> getNimbusConf() {
        List<String> args = new ArrayList<>();

        // FIXME: Can't find how to pass list to nimbus.seeds for Storm CLI
        // Maybe we need to fix Storm to parse string when expected parameter type is list
        args.add("-c");
        args.add("nimbus.host=" + nimbusSeeds.split(",")[0]);

        args.add("-c");
        args.add("nimbus.port=" + String.valueOf(nimbusPort));

        return args;
    }

    private List<String> getSecuredClusterConf(String asUser) {
        List<String> args = new ArrayList<>();
        args.add("-c");
        args.add("storm.thrift.transport=" + thriftTransport);

        if (jaasFilePath.isPresent()) {
            args.add("-c");
            args.add("java.security.auth.login.config=" + jaasFilePath.get());
        }

        args.add("-c");
        args.add("storm.principal.tolocal=" + principalToLocal);

        if (StringUtils.isNotEmpty(asUser)) {
            args.add("-c");
            args.add("storm.doAsUser=" + asUser);
        }

        return args;
    }

    private String createJaasFile(String kerberizedNimbusServiceName) {
        try {
            Path jaasFilePath = Files.createTempFile("jaas-", UUID.randomUUID().toString());

            String filePath = jaasFilePath.toAbsolutePath().toString();
            File jaasFile = new StormJaasCreator().create(filePath, kerberizedNimbusServiceName);
            return jaasFile.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Can't create JAAS file to connect to secure nimbus", e);
        }
    }

    private Path addArtifactsToJar(Path artifactsLocation) throws Exception {
        Path jarFile = Paths.get(stormJarLocation);
        if (artifactsLocation.toFile().isDirectory()) {
            File[] artifacts = artifactsLocation.toFile().listFiles();
            if (artifacts != null && artifacts.length > 0) {
                Path newJar = Files.copy(jarFile, artifactsLocation.resolve(jarFile.getFileName()));

                List<String> artifactFileNames = Arrays.stream(artifacts).filter(File::isFile)
                        .map(File::getName).collect(toList());
                List<String> commands = new ArrayList<>();

                commands.add(javaJarCommand);
                commands.add("uf");
                commands.add(newJar.toString());

                artifactFileNames.stream().forEachOrdered(name -> {
                    commands.add("-C");
                    commands.add(artifactsLocation.toString());
                    commands.add(name);
                });

                ShellProcessResult shellProcessResult = executeShellProcess(commands);
                if (shellProcessResult.exitValue != 0) {
                    LOG.error("Adding artifacts to jar command failed - exit code: {} / output: {}",
                            shellProcessResult.exitValue, shellProcessResult.stdout);
                    throw new RuntimeException("Topology could not be deployed " +
                            "successfully: fail to add artifacts to jar");
                }
                LOG.debug("Added files {} to jar {}", artifactFileNames, jarFile);
                return newJar;
            }
        } else {
            LOG.debug("Artifacts directory {} does not exist, not adding any artifacts to jar", artifactsLocation);
        }
        return jarFile;
    }

    private String createYamlFile (TopologyLayout topology) throws Exception {
        Map<String, Object> yamlMap;
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
            yamlMap = new LinkedHashMap<>();
            yamlMap.put(StormTopologyLayoutConstants.YAML_KEY_NAME, generateStormTopologyName(topology));
            TopologyDag topologyDag = topology.getTopologyDag();
            LOG.debug("Initial Topology config {}", topology.getConfig());
            StormTopologyFluxGenerator fluxGenerator = new StormTopologyFluxGenerator(topology, conf, getExtraJarsLocation(topology));
            topologyDag.traverse(fluxGenerator);
            for (Map.Entry<String, Map<String, Object>> entry: fluxGenerator.getYamlKeysAndComponents()) {
                addComponentToCollection(yamlMap, entry.getValue(), entry.getKey());
            }
            Config topologyConfig = fluxGenerator.getTopologyConfig();
            LOG.debug("Final Topology config {}", topologyConfig);
            addTopologyConfig(yamlMap, topologyConfig.getProperties());
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setSplitLines(false);
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

    private String generateStormTopologyName(TopologyLayout topology) {
        return StormTopologyUtil.generateStormTopologyName(topology.getId(), topology.getName());
    }

    private String getFilePath (TopologyLayout topology) {
        return this.stormArtifactsLocation + generateStormTopologyName(topology) + ".yaml";
    }

    // Add topology level configs. catalogRootUrl, hbaseConf, hdfsConf,
    // numWorkers, etc.
    private void addTopologyConfig (Map<String, Object> yamlMap, Map<String, Object> topologyConfig) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put(StormTopologyLayoutConstants.YAML_KEY_CATALOG_ROOT_URL, catalogRootUrl);
        if (topologyConfig != null) {
            config.putAll(topologyConfig);
        }
        yamlMap.put(StormTopologyLayoutConstants.YAML_KEY_CONFIG, config);
    }

    private void addComponentToCollection (Map<String, Object> yamlMap, Map<String, Object> yamlComponent, String collectionKey) {
        if (yamlComponent == null ) {
            return;
        }

        List<Map<String, Object>> components = (List<Map<String, Object>>) yamlMap.get(collectionKey);
        if (components == null) {
            components = new ArrayList<>();
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
        IOUtils.copy(process.getInputStream(), sw, Charset.defaultCharset());
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
