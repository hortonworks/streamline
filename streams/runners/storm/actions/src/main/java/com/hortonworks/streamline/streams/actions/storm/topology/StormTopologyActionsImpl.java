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
import com.google.common.collect.Lists;
import com.hortonworks.streamline.common.Config;
import com.hortonworks.streamline.common.exception.service.exception.request.TopologyAlreadyExistsOnCluster;
import com.hortonworks.streamline.streams.actions.StatusImpl;
import com.hortonworks.streamline.streams.actions.TopologyActionContext;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunHistory;
import com.hortonworks.streamline.streams.cluster.Constants;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.exception.TopologyNotAliveException;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.layout.component.InputComponent;
import com.hortonworks.streamline.streams.layout.component.OutputComponent;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import com.hortonworks.streamline.streams.layout.component.impl.HBaseSink;
import com.hortonworks.streamline.streams.layout.component.impl.HdfsSink;
import com.hortonworks.streamline.streams.layout.component.impl.HdfsSource;
import com.hortonworks.streamline.streams.layout.component.impl.HiveSink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunRulesProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSource;
import com.hortonworks.streamline.streams.layout.storm.StormTopologyFluxGenerator;
import com.hortonworks.streamline.streams.layout.storm.StormTopologyLayoutConstants;
import com.hortonworks.streamline.streams.layout.storm.StormTopologyValidator;
import com.hortonworks.streamline.streams.storm.common.StormJaasCreator;
import com.hortonworks.streamline.streams.storm.common.StormRestAPIClient;
import com.hortonworks.streamline.streams.storm.common.StormTopologyUtil;
import com.hortonworks.streamline.streams.storm.common.logger.LogLevelLoggerResponse;
import com.hortonworks.streamline.streams.storm.common.logger.LogLevelResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.security.auth.Subject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
    public static final int TEST_RUN_TOPOLOGY_DEFAULT_WAIT_MILLIS_FOR_SHUTDOWN = 30_000;
    public static final String ROOT_LOGGER_NAME = "ROOT";

    private static final String DEFAULT_THRIFT_TRANSPORT_PLUGIN = "org.apache.storm.security.auth.SimpleTransportPlugin";
    private static final String DEFAULT_PRINCIPAL_TO_LOCAL = "org.apache.storm.security.auth.DefaultPrincipalToLocal";

    private static final String NIMBUS_SEEDS = "nimbus.seeds";
    private static final String NIMBUS_PORT = "nimbus.port";

    public static final String STREAMLINE_TOPOLOGY_CONFIG_CLUSTER_SECURITY_CONFIG = "clustersSecurityConfig";
    public static final String STREAMLINE_TOPOLOGY_CONFIG_CLUSTER_ID = "clusterId";
    public static final String STREAMLINE_TOPOLOGY_CONFIG_PRINCIPAL = "principal";
    public static final String STREAMLINE_TOPOLOGY_CONFIG_KEYTAB_PATH = "keytabPath";

    public static final String STORM_TOPOLOGY_CONFIG_AUTO_CREDENTIALS = "topology.auto-credentials";
    public static final String TOPOLOGY_CONFIG_KEY_CLUSTER_KEY_PREFIX_HDFS = "hdfs_";
    public static final String TOPOLOGY_CONFIG_KEY_CLUSTER_KEY_PREFIX_HBASE = "hbase_";
    public static final String TOPOLOGY_CONFIG_KEY_CLUSTER_KEY_PREFIX_HIVE = "hive_";
    public static final String TOPOLOGY_CONFIG_KEY_HDFS_KEYTAB_FILE = "hdfs.keytab.file";
    public static final String TOPOLOGY_CONFIG_KEY_HBASE_KEYTAB_FILE = "hbase.keytab.file";
    public static final String TOPOLOGY_CONFIG_KEY_HIVE_KEYTAB_FILE = "hive.keytab.file";
    public static final String TOPOLOGY_CONFIG_KEY_HDFS_KERBEROS_PRINCIPAL = "hdfs.kerberos.principal";
    public static final String TOPOLOGY_CONFIG_KEY_HBASE_KERBEROS_PRINCIPAL = "hbase.kerberos.principal";
    public static final String TOPOLOGY_CONFIG_KEY_HIVE_KERBEROS_PRINCIPAL = "hive.kerberos.principal";
    public static final String TOPOLOGY_CONFIG_KEY_HDFS_CREDENTIALS_CONFIG_KEYS = "hdfsCredentialsConfigKeys";
    public static final String TOPOLOGY_CONFIG_KEY_HBASE_CREDENTIALS_CONFIG_KEYS = "hbaseCredentialsConfigKeys";
    public static final String TOPOLOGY_CONFIG_KEY_HIVE_CREDENTIALS_CONFIG_KEYS = "hiveCredentialsConfigKeys";
    public static final String TOPOLOGY_AUTO_CREDENTIAL_CLASSNAME_HDFS = "org.apache.storm.hdfs.security.AutoHDFS";
    public static final String TOPOLOGY_AUTO_CREDENTIAL_CLASSNAME_HBASE = "org.apache.storm.hbase.security.AutoHBase";
    public static final String TOPOLOGY_AUTO_CREDENTIAL_CLASSNAME_HIVE = "org.apache.storm.hive.security.AutoHive";

    private static final Long DEFAULT_NIMBUS_THRIFT_MAX_BUFFER_SIZE = 1048576L;

    public static final String TOPOLOGY_EVENTLOGGER_REGISTER = "topology.event.logger.register";
    public static final String TOPOLOGY_EVENTLOGGER_CLASSNAME_STREAMLINE = "com.hortonworks.streamline.streams.runtime.storm.event.sample.StreamlineEventLogger";

    private String stormArtifactsLocation = "/tmp/storm-artifacts/";
    private String stormCliPath = "storm";
    private String stormJarLocation;
    private String catalogRootUrl;
    private String javaJarCommand;
    private StormRestAPIClient client;
    private String nimbusSeeds;
    private Integer nimbusPort;
    private Map<String, Object> conf;

    private String thriftTransport;
    private Optional<String> jaasFilePath;
    private String principalToLocal;
    private long nimbusThriftMaxBufferSize;

    private AutoCredsServiceConfigurationReader serviceConfigurationReader;
    private final ConcurrentHashMap<Long, Boolean> forceKillRequests = new ConcurrentHashMap<>();

    public StormTopologyActionsImpl() {
    }

    @Override
    public void init (Map<String, Object> conf) {
        this.conf = conf;
        if (conf != null) {
            if (conf.containsKey(StormTopologyLayoutConstants.STORM_ARTIFACTS_LOCATION_KEY)) {
                stormArtifactsLocation = (String) conf.get(StormTopologyLayoutConstants.STORM_ARTIFACTS_LOCATION_KEY);
            }
            if (conf.containsKey(StormTopologyLayoutConstants.STORM_HOME_DIR)) {
                String stormHomeDir = (String) conf.get(StormTopologyLayoutConstants.STORM_HOME_DIR);
                if (!stormHomeDir.endsWith(File.separator)) {
                    stormHomeDir += File.separator;
                }
                stormCliPath = stormHomeDir + "bin" + File.separator + "storm";
            }
            this.stormJarLocation = (String) conf.get(StormTopologyLayoutConstants.STORM_JAR_LOCATION_KEY);

            catalogRootUrl = (String) conf.get(StormTopologyLayoutConstants.YAML_KEY_CATALOG_ROOT_URL);

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

            String stormApiRootUrl = (String) conf.get(TopologyLayoutConstants.STORM_API_ROOT_URL_KEY);
            Subject subject = (Subject) conf.get(TopologyLayoutConstants.SUBJECT_OBJECT);
            Client restClient = ClientBuilder.newClient(new ClientConfig());

            this.client = new StormRestAPIClient(restClient, stormApiRootUrl, subject);
            nimbusSeeds = (String) conf.get(NIMBUS_SEEDS);
            nimbusPort = Integer.valueOf((String) conf.get(NIMBUS_PORT));

            if (conf.containsKey(TopologyLayoutConstants.NIMBUS_THRIFT_MAX_BUFFER_SIZE)) {
                nimbusThriftMaxBufferSize = (Long) conf.get(TopologyLayoutConstants.NIMBUS_THRIFT_MAX_BUFFER_SIZE);
            } else {
                nimbusThriftMaxBufferSize = DEFAULT_NIMBUS_THRIFT_MAX_BUFFER_SIZE;
            }

            setupSecuredStormCluster(conf);

            EnvironmentService environmentService = (EnvironmentService) conf.get(TopologyLayoutConstants.ENVIRONMENT_SERVICE_OBJECT);
            Number namespaceId = (Number) conf.get(TopologyLayoutConstants.NAMESPACE_ID);
            this.serviceConfigurationReader = new AutoCredsServiceConfigurationReader(environmentService,
                    namespaceId.longValue());
        }
        File f = new File (stormArtifactsLocation);
        if (!f.exists() && !f.mkdirs()) {
            throw new RuntimeException("Could not create directory " + f.getAbsolutePath());
        }
    }

    private void setupSecuredStormCluster(Map<String, Object> conf) {
        thriftTransport = (String) conf.get(TopologyLayoutConstants.STORM_THRIFT_TRANSPORT);

        if (conf.containsKey(TopologyLayoutConstants.STORM_NIMBUS_PRINCIPAL_NAME)) {
            String nimbusPrincipal = (String) conf.get(TopologyLayoutConstants.STORM_NIMBUS_PRINCIPAL_NAME);
            String kerberizedNimbusServiceName = nimbusPrincipal.split("/")[0];
            jaasFilePath = Optional.of(createJaasFile(kerberizedNimbusServiceName));
        } else {
            jaasFilePath = Optional.empty();
        }

        principalToLocal = (String) conf.getOrDefault(TopologyLayoutConstants.STORM_PRINCIPAL_TO_LOCAL, DEFAULT_PRINCIPAL_TO_LOCAL);

        if (thriftTransport == null) {
            if (jaasFilePath.isPresent()) {
                thriftTransport = (String) conf.get(TopologyLayoutConstants.STORM_SECURED_THRIFT_TRANSPORT);
            } else {
                thriftTransport = (String) conf.get(TopologyLayoutConstants.STORM_NONSECURED_THRIFT_TRANSPORT);
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
        LOG.info("Deploying Application {}", topology.getName());
        LOG.info(String.join(" ", commands));

        Process process = executeShellProcess(commands);
        ShellProcessResult shellProcessResult = waitProcessFor(process);
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
    public void runTest(TopologyLayout topology, TopologyTestRunHistory testRunHistory, String mavenArtifacts,
                        Map<String, TestRunSource> testRunSourcesForEachSource,
                        Map<String, TestRunProcessor> testRunProcessorsForEachProcessor,
                        Map<String, TestRunRulesProcessor> testRunRulesProcessorsForEachProcessor,
                        Map<String, TestRunSink> testRunSinksForEachSink, Optional<Long> durationSecs) throws Exception {
        TopologyDag originalTopologyDag = topology.getTopologyDag();

        TestTopologyDagCreatingVisitor visitor = new TestTopologyDagCreatingVisitor(originalTopologyDag,
                testRunSourcesForEachSource, testRunProcessorsForEachProcessor, testRunRulesProcessorsForEachProcessor,
                testRunSinksForEachSink);
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
        commands.addAll(getTempWorkerArtifactArgs());
        commands.add("org.apache.storm.flux.Flux");
        commands.add("--local");

        commands.add("-s");
        if (durationSecs.isPresent()) {
            commands.add(String.valueOf(durationSecs.get() * 1000));
        } else {
            commands.add(String.valueOf(TEST_RUN_TOPOLOGY_DEFAULT_WAIT_MILLIS_FOR_SHUTDOWN));
        }

        commands.add(fileName);

        Process process = executeShellProcess(commands);
        ShellProcessResult shellProcessResult = waitTestRunProcess(process, testRunHistory.getId());
        int exitValue = shellProcessResult.exitValue;
        if (exitValue != 0) {
            LOG.error("Topology deploy command as test mode failed - exit code: {} / output: {}", exitValue, shellProcessResult.stdout);
            throw new Exception("Topology could not be run " +
                    "successfully as test mode: storm deploy command failed");
        }
    }

    @Override
    public boolean killTest(TopologyTestRunHistory testRunHistory) {
        // just turn on the flag only if it exists
        LOG.info("Turning on force kill flag on test run history {}", testRunHistory.getId());
        Boolean newValue = forceKillRequests.computeIfPresent(testRunHistory.getId(), (id, flag) -> true);
        return newValue != null;
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

    @Override
    public LogLevelInformation configureLogLevel(TopologyLayout topology, LogLevel targetLogLevel, int durationSecs, String asUser) throws Exception {
        String stormTopologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        if (StringUtils.isEmpty(stormTopologyId)) {
            return null;
        }
        LogLevelResponse response = client.configureLog(stormTopologyId, ROOT_LOGGER_NAME, targetLogLevel.name(),
                durationSecs, asUser);
        return convertLogLevelResponseToLogLevelInformation(response);
    }

    @Override
    public LogLevelInformation getLogLevel(TopologyLayout topology, String asUser) throws Exception {
        String stormTopologyId = StormTopologyUtil.findStormTopologyId(client, topology.getId(), asUser);
        if (StringUtils.isEmpty(stormTopologyId)) {
            return null;
        }
        LogLevelResponse response = client.getLogLevel(stormTopologyId, asUser);
        return convertLogLevelResponseToLogLevelInformation(response);
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
            args.add((String) conf.get("mavenRepoUrl"));

            String proxyUrl = (String) conf.get("proxyUrl");
            if (StringUtils.isNotEmpty(proxyUrl)) {
                args.add("--proxyUrl");
                args.add(proxyUrl);

                String proxyUsername = (String) conf.get("proxyUsername");
                String proxyPassword = (String) conf.get("proxyPassword");
                if (proxyUsername != null && proxyPassword != null) {
                    // allow empty string but not null
                    args.add("--proxyUsername");
                    args.add(proxyUsername);
                    args.add("--proxyPassword");
                    args.add(proxyPassword);
                }
            }
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

        args.add("-c");
        args.add("nimbus.thrift.max_buffer_size=" + String.valueOf(nimbusThriftMaxBufferSize));

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

    private List<String> getTempWorkerArtifactArgs() throws IOException {
        List<String> args = new ArrayList<>();

        Path tempArtifacts = Files.createTempDirectory("worker-artifacts-");
        args.add("-c");
        args.add("storm.workers.artifacts.dir=" + tempArtifacts.toFile().getAbsolutePath());

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

                Process process = executeShellProcess(commands);
                ShellProcessResult shellProcessResult = waitProcessFor(process);
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
        OutputStreamWriter fileWriter = null;
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
            StormTopologyFluxGenerator fluxGenerator = new StormTopologyFluxGenerator(topology, conf,
                    getExtraJarsLocation(topology));
            topologyDag.traverse(fluxGenerator);
            for (Map.Entry<String, Map<String, Object>> entry: fluxGenerator.getYamlKeysAndComponents()) {
                addComponentToCollection(yamlMap, entry.getValue(), entry.getKey());
            }
            Config topologyConfig = fluxGenerator.getTopologyConfig();
            putAutoTokenDelegationConfig(topologyConfig, topologyDag);
            registerEventLogger(topologyConfig);

            LOG.debug("Final Topology config {}", topologyConfig);
            addTopologyConfig(yamlMap, topologyConfig.getProperties());
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setSplitLines(false);
            //options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
            Yaml yaml = new Yaml (options);
            fileWriter = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
            yaml.dump(yamlMap, fileWriter);
            return f.getAbsolutePath();
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }

    private void registerEventLogger(Config topologyConfig) {
        topologyConfig.put(TOPOLOGY_EVENTLOGGER_REGISTER,
                Collections.singletonList(
                        Collections.singletonMap("class", TOPOLOGY_EVENTLOGGER_CLASSNAME_STREAMLINE))
        );
    }

    private void putAutoTokenDelegationConfig(Config topologyConfig, TopologyDag topologyDag) {
        Optional<?> securityConfigsOptional = topologyConfig.getAnyOptional(STREAMLINE_TOPOLOGY_CONFIG_CLUSTER_SECURITY_CONFIG);
        Map<Long, Map<String, String>> clusterToConfiguration = new HashMap<>();
        if (securityConfigsOptional.isPresent()) {
            List<?> securityConfigurations = (List<?>) securityConfigsOptional.get();
            securityConfigurations.forEach(securityConfig -> {
                Map<String, Object> sc = (Map<String, Object>) securityConfig;
                if ((sc != null) && !sc.isEmpty()) {
                    Long clusterId = ((Number) sc.get(STREAMLINE_TOPOLOGY_CONFIG_CLUSTER_ID)).longValue();

                    Map<String, String> configurationForCluster = new HashMap<>();
                    configurationForCluster.put(STREAMLINE_TOPOLOGY_CONFIG_PRINCIPAL, (String) sc.get(STREAMLINE_TOPOLOGY_CONFIG_PRINCIPAL));
                    configurationForCluster.put(STREAMLINE_TOPOLOGY_CONFIG_KEYTAB_PATH, (String) sc.get(STREAMLINE_TOPOLOGY_CONFIG_KEYTAB_PATH));
                    clusterToConfiguration.put(clusterId, configurationForCluster);
                }
            });
        }

        if (clusterToConfiguration.isEmpty()) {
            // it will function only when user input keytab path and principal
            return;
        }

        // Hive also needs HDFS auto token delegation, so HiveSink is added to the checklist
        boolean hdfsCredentialNecessary = checkTopologyContainingServiceRelatedComponent(topologyDag,
                Collections.singletonList(HdfsSource.class),
                Lists.newArrayList(HdfsSink.class, HiveSink.class));

        boolean hbaseCredentialNecessary = checkTopologyContainingServiceRelatedComponent(topologyDag,
                Collections.emptyList(),
                Collections.singletonList(HBaseSink.class));

        boolean hiveCredentialNecessary = checkTopologyContainingServiceRelatedComponent(topologyDag,
                Collections.emptyList(),
                Collections.singletonList(HiveSink.class));

        if (hdfsCredentialNecessary) {
            putServiceSpecificCredentialConfig(topologyConfig, clusterToConfiguration,
                    Constants.HDFS.SERVICE_NAME,
                    Collections.emptyList(),
                    TOPOLOGY_CONFIG_KEY_CLUSTER_KEY_PREFIX_HDFS, TOPOLOGY_CONFIG_KEY_HDFS_KEYTAB_FILE,
                    TOPOLOGY_CONFIG_KEY_HDFS_KERBEROS_PRINCIPAL,
                    TOPOLOGY_CONFIG_KEY_HDFS_CREDENTIALS_CONFIG_KEYS, TOPOLOGY_AUTO_CREDENTIAL_CLASSNAME_HDFS);
        }

        if (hbaseCredentialNecessary) {
            putServiceSpecificCredentialConfig(topologyConfig, clusterToConfiguration,
                    Constants.HBase.SERVICE_NAME,
                    Collections.singletonList(Constants.HDFS.SERVICE_NAME),
                    TOPOLOGY_CONFIG_KEY_CLUSTER_KEY_PREFIX_HBASE, TOPOLOGY_CONFIG_KEY_HBASE_KEYTAB_FILE,
                    TOPOLOGY_CONFIG_KEY_HBASE_KERBEROS_PRINCIPAL,
                    TOPOLOGY_CONFIG_KEY_HBASE_CREDENTIALS_CONFIG_KEYS, TOPOLOGY_AUTO_CREDENTIAL_CLASSNAME_HBASE);
        }

        if (hiveCredentialNecessary) {
            putServiceSpecificCredentialConfig(topologyConfig, clusterToConfiguration,
                    Constants.Hive.SERVICE_NAME,
                    Collections.singletonList(Constants.HDFS.SERVICE_NAME),
                    TOPOLOGY_CONFIG_KEY_CLUSTER_KEY_PREFIX_HIVE, TOPOLOGY_CONFIG_KEY_HIVE_KEYTAB_FILE,
                    TOPOLOGY_CONFIG_KEY_HIVE_KERBEROS_PRINCIPAL,
                    TOPOLOGY_CONFIG_KEY_HIVE_CREDENTIALS_CONFIG_KEYS, TOPOLOGY_AUTO_CREDENTIAL_CLASSNAME_HIVE);
        }
    }

    public boolean checkTopologyContainingServiceRelatedComponent(TopologyDag topologyDag,
                                                                  List<Class<?>> outputComponentClasses,
                                                                  List<Class<?>> inputComponentClasses) {
        boolean componentExists = false;
        for (OutputComponent outputComponent : topologyDag.getOutputComponents()) {
            for (Class<?> clazz : outputComponentClasses) {
                if (outputComponent.getClass().isAssignableFrom(clazz)) {
                    componentExists = true;
                    break;
                }
            }
        }

        if (!componentExists) {
            for (InputComponent inputComponent : topologyDag.getInputComponents()) {
                for (Class<?> clazz : inputComponentClasses) {
                    if (inputComponent.getClass().isAssignableFrom(clazz)) {
                        componentExists = true;
                        break;
                    }
                }
            }
        }

        return componentExists;
    }

    private void putServiceSpecificCredentialConfig(Config topologyConfig,
                                                    Map<Long, Map<String, String>> clusterToConfiguration,
                                                    String serviceName,
                                                    List<String> dependentServiceNames,
                                                    String clusterKeyPrefix,
                                                    String keytabPathKeyName, String principalKeyName,
                                                    String credentialConfigKeyName,
                                                    String topologyAutoCredentialClassName) {
        List<String> clusterKeys = new ArrayList<>();

        clusterToConfiguration.keySet()
                .forEach(clusterId -> {
                    Map<String, String> conf = serviceConfigurationReader.read(clusterId, serviceName);
                    // add only when such (cluster, service) pair is available for the namespace
                    if (!conf.isEmpty()) {
                        Map<String, String> confForToken = clusterToConfiguration.get(clusterId);
                        conf.put(principalKeyName, confForToken.get(STREAMLINE_TOPOLOGY_CONFIG_PRINCIPAL));
                        conf.put(keytabPathKeyName, confForToken.get(STREAMLINE_TOPOLOGY_CONFIG_KEYTAB_PATH));

                        // also includes all configs for dependent services
                        // note that such services in cluster should also be associated to the namespace
                        Map<String, String> clusterConf = new HashMap<>();
                        dependentServiceNames.forEach(depSvcName -> {
                            Map<String, String> depConf = serviceConfigurationReader.read(clusterId, depSvcName);
                            clusterConf.putAll(depConf);
                        });
                        clusterConf.putAll(conf);

                        String clusterKey = clusterKeyPrefix + clusterId;
                        topologyConfig.put(clusterKey, clusterConf);
                        clusterKeys.add(clusterKey);
                    }
                });

        topologyConfig.put(credentialConfigKeyName, clusterKeys);

        Optional<List<String>> autoCredentialsOptional = topologyConfig.getAnyOptional(STORM_TOPOLOGY_CONFIG_AUTO_CREDENTIALS);
        if (autoCredentialsOptional.isPresent()) {
            List<String> autoCredentials = autoCredentialsOptional.get();
            autoCredentials.add(topologyAutoCredentialClassName);
        } else {
            topologyConfig.put(STORM_TOPOLOGY_CONFIG_AUTO_CREDENTIALS, Lists.newArrayList(topologyAutoCredentialClassName));
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
        //Hack to work around HBaseBolt expecting this map in prepare method. Fix HBaseBolt and get rid of this. We use hbase-site.xml conf from ambari
        config.put(StormTopologyLayoutConstants.YAML_KEY_HBASE_CONF, new HashMap<>());
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

    private Process executeShellProcess (List<String> commands) throws Exception {
        LOG.debug("Executing command: {}", Joiner.on(" ").join(commands));
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.redirectErrorStream(true);
        return processBuilder.start();
    }

    private ShellProcessResult waitProcessFor(Process process) throws IOException, InterruptedException {
        StringWriter sw = new StringWriter();
        IOUtils.copy(process.getInputStream(), sw, Charset.defaultCharset());
        String stdout = sw.toString();
        process.waitFor();
        int exitValue = process.exitValue();
        LOG.debug("Command output: {}", stdout);
        LOG.debug("Command exit status: {}", exitValue);
        return new ShellProcessResult(exitValue, stdout);
    }

    private ShellProcessResult waitTestRunProcess(Process process, long topologyRunHistoryId) throws IOException {
        forceKillRequests.put(topologyRunHistoryId, false);

        LOG.info("Waiting for test run for history {} to be finished...", topologyRunHistoryId);

        StringBuilder sb = new StringBuilder();
        for (int idx = 0 ; ; idx++) {
            if (!process.isAlive()) {
                break;
            }

            // read stdout if available
            sb.append(readProcessStream(process.getInputStream()));

            if (forceKillRequests.getOrDefault(topologyRunHistoryId, false)) {
                LOG.info("Received force kill for test run {} - destroying process...", topologyRunHistoryId);
                process.destroyForcibly();
            }

            // leave a log for each 10 secs...
            if ((idx + 1) % 100 == 0) {
                LOG.info("Still waiting for test run for history {} to be finished...", topologyRunHistoryId);
            }

            try {
                process.waitFor(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // ignored
            }
        }

        LOG.info("Test run for history {} is finished", topologyRunHistoryId);

        String stdout = sb.toString();

        forceKillRequests.remove(topologyRunHistoryId);

        int exitValue = process.exitValue();
        LOG.debug("Command output: {}", stdout);
        LOG.debug("Command exit status: {}", exitValue);
        return new ShellProcessResult(exitValue, stdout);
    }

    private String readProcessStream(InputStream processInputStream) {
        try {
            StringBuilder sb = new StringBuilder();
            while (processInputStream.available() > 0) {
                int bufferSize = processInputStream.available();
                byte[] errorReadingBuffer = new byte[bufferSize];
                int readSize = processInputStream.read(errorReadingBuffer, 0, bufferSize);
                errorReadingBuffer = Arrays.copyOfRange(errorReadingBuffer, 0, readSize);
                sb.append(new String(errorReadingBuffer, Charset.forName("UTF-8")));
            }
            return sb.toString();
        } catch (IOException e) {
            return "(cannot capture process stream)";
        }
    }

    private static class ShellProcessResult {
        private final int exitValue;
        private final String stdout;
        ShellProcessResult(int exitValue, String stdout) {
            this.exitValue = exitValue;
            this.stdout = stdout;
        }
    }

    private LogLevelInformation convertLogLevelResponseToLogLevelInformation(LogLevelResponse response) {
        Map<String, LogLevelLoggerResponse> namedLoggerLevels = response.getNamedLoggerLevels();
        LogLevelLoggerResponse resp = namedLoggerLevels.get(ROOT_LOGGER_NAME);
        if (resp == null) {
            return LogLevelInformation.disabled();
        }

        return LogLevelInformation.enabled(LogLevel.valueOf(resp.getTargetLevel()), resp.getTimeoutEpoch());
    }

}
