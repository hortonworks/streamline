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
package com.hortonworks.streamline.streams.actions.topology.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.registries.common.transaction.TransactionIsolation;
import com.hortonworks.registries.common.util.FileStorage;
import com.hortonworks.registries.storage.TransactionManager;
import com.hortonworks.streamline.registries.model.client.MLModelRegistryClient;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.container.TopologyActionsContainer;
import com.hortonworks.streamline.streams.actions.topology.state.TopologyContext;
import com.hortonworks.streamline.streams.actions.topology.state.TopologyState;
import com.hortonworks.streamline.streams.actions.topology.state.TopologyStateFactory;
import com.hortonworks.streamline.streams.actions.topology.state.TopologyStates;
import com.hortonworks.streamline.streams.catalog.CatalogToLayoutConverter;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunCase;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.cluster.catalog.NamespaceServiceClusterMap;
import com.hortonworks.streamline.streams.cluster.catalog.Service;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyTestRunHistory;
import com.hortonworks.streamline.common.configuration.ConfigFileType;
import com.hortonworks.streamline.common.configuration.ConfigFileWriter;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.catalog.topology.TopologyComponentBundle;
import com.hortonworks.streamline.streams.catalog.topology.component.TopologyDagBuilder;
import com.hortonworks.streamline.streams.cluster.container.ContainingNamespaceAwareContainer;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.layout.component.OutputComponent;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.hortonworks.streamline.streams.actions.topology.state.TopologyStates.TOPOLOGY_STATE_INITIAL;

public class TopologyActionsService implements ContainingNamespaceAwareContainer {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyActionsService.class);

    public static final String TOPOLOGY_TEST_RUN_RESULT_DIR = "topologyTestRunResultDir";

    private final StreamCatalogService catalogService;
    private final EnvironmentService environmentService;
    private final TopologyDagBuilder topologyDagBuilder;
    private final ConfigFileWriter configFileWriter;
    private final FileStorage fileStorage;
    private final TopologyActionsContainer topologyActionsContainer;
    private final TopologyStateFactory stateFactory;
    private final TopologyTestRunner topologyTestRunner;
    private final TransactionManager transactionManager;

    public TopologyActionsService(StreamCatalogService catalogService, EnvironmentService environmentService,
                                  FileStorage fileStorage, MLModelRegistryClient modelRegistryClient,
                                  Map<String, Object> configuration, Subject subject, TransactionManager transactionManager) {
        this.catalogService = catalogService;
        this.environmentService = environmentService;
        this.fileStorage = fileStorage;
        this.topologyDagBuilder = new TopologyDagBuilder(catalogService, modelRegistryClient);
        this.configFileWriter = new ConfigFileWriter();

        Map<String, String> conf = new HashMap<>();
        for (Map.Entry<String, Object> confEntry : configuration.entrySet()) {
            Object value = confEntry.getValue();
            conf.put(confEntry.getKey(), value == null ? null : value.toString());
        }

        String topologyTestRunResultDir = conf.get(TOPOLOGY_TEST_RUN_RESULT_DIR);
        if (StringUtils.isEmpty(topologyTestRunResultDir)) {
            throw new RuntimeException("Configuration of topology test run result dir is not set.");
        }

        if (topologyTestRunResultDir.endsWith(File.separator)) {
            topologyTestRunResultDir = topologyTestRunResultDir.substring(0, topologyTestRunResultDir.length() - 1);
        }

        this.topologyActionsContainer = new TopologyActionsContainer(environmentService, conf, subject);
        this.stateFactory = TopologyStateFactory.getInstance();
        this.topologyTestRunner = new TopologyTestRunner(catalogService, this, topologyTestRunResultDir);
        this.transactionManager = transactionManager;
    }

    public Void deployTopology(Topology topology, String asUser) throws Exception {
        TopologyContext ctx;
        try {
            transactionManager.beginTransaction(TransactionIsolation.DEFAULT);
            ctx = getTopologyContext(topology, asUser);
            transactionManager.commitTransaction();
        } catch (Exception e){
            transactionManager.rollbackTransaction();
            throw e;
        }
        LOG.debug("Deploying topology {}", topology);
        while (ctx.getState() != TopologyStates.TOPOLOGY_STATE_DEPLOYED) {
            try {
                transactionManager.beginTransaction(TransactionIsolation.DEFAULT);
                LOG.debug("Current state {}", ctx.getStateName());
                ctx.deploy();
                transactionManager.commitTransaction();
            } catch (Exception e){
                transactionManager.rollbackTransaction();
                throw e;
            }
        }
        return null;
    }

    public TopologyTestRunHistory testRunTopology(Topology topology, TopologyTestRunCase testCase, Long durationSecs) throws Exception {
        TopologyDag dag = topologyDagBuilder.getDag(topology);
        topology.setTopologyDag(dag);
        ensureValid(dag);
        TopologyActions topologyActions = getTopologyActionsInstance(topology);
        LOG.debug("Running topology {} in test mode", topology);
        return topologyTestRunner.runTest(topologyActions, topology, testCase, durationSecs);
    }

    public boolean killTestRunTopology(Topology topology, TopologyTestRunHistory history) {
        TopologyActions topologyActions = getTopologyActionsInstance(topology);
        return topologyTestRunner.killTest(topologyActions, history);
    }

    public void killTopology(Topology topology, String asUser) throws Exception {
        getTopologyContext(topology, asUser).kill();
    }

    public void suspendTopology(Topology topology, String asUser) throws Exception {
        getTopologyContext(topology, asUser).suspend();
    }

    public void resumeTopology(Topology topology, String asUser) throws Exception {
        getTopologyContext(topology, asUser).resume();
    }

    public TopologyActions.Status topologyStatus(Topology topology, String asUser) throws Exception {
        TopologyActions topologyActions = getTopologyActionsInstance(topology);
        return topologyActions.status(CatalogToLayoutConverter.getTopologyLayout(topology), asUser);
    }

    public String getRuntimeTopologyId(Topology topology, String asUser) throws IOException {
        TopologyActions topologyActions = getTopologyActionsInstance(topology);
        return topologyActions.getRuntimeTopologyId(CatalogToLayoutConverter.getTopologyLayout(topology), asUser);
    }

    @Override
    public void invalidateInstance(Long namespaceId) {
        try {
            topologyActionsContainer.invalidateInstance(namespaceId);
        } catch (Throwable e) {
            // swallow
        }
    }

    /* Should have
     * at-least 1 processor
     * OR
     * at-least 1 source with an edge.
     */
    public void ensureValid(TopologyDag dag) {
        if (dag.getComponents().isEmpty()) {
            throw new IllegalStateException("Empty topology");
        }
        java.util.Optional<OutputComponent> processor = dag.getOutputComponents().stream()
                .filter(x -> x instanceof StreamlineProcessor)
                .findFirst();
        if (!processor.isPresent()) {
            java.util.Optional<OutputComponent> sourceWithOutgoingEdge = dag.getOutputComponents().stream()
                    .filter(x -> x instanceof StreamlineSource && !dag.getEdgesFrom(x).isEmpty())
                    .findFirst();
            if (!sourceWithOutgoingEdge.isPresent()) {
                throw new IllegalStateException("Topology does not contain a processor or a source with an outgoing edge");
            }
        }
    }

    // Copies all the extra jars needed for deploying the topology to extraJarsLocation and returns
    // a string representing all additional maven modules needed for deploying the topology
    public String setUpExtraJars(Topology topology, TopologyActions topologyActions) throws IOException {
        StormTopologyDependenciesHandler extraJarsHandler = new StormTopologyDependenciesHandler(catalogService);
        topology.getTopologyDag().traverse(extraJarsHandler);
        Path extraJarsLocation = topologyActions.getExtraJarsLocation(CatalogToLayoutConverter.getTopologyLayout(topology));
        makeEmptyDir(extraJarsLocation);
        Set<String> extraJars = new HashSet<>();
        extraJars.addAll(extraJarsHandler.getExtraJars());
        extraJars.addAll(getBundleJars(extraJarsHandler.getTopologyComponentBundleSet()));
        downloadAndCopyJars(extraJars, extraJarsLocation);
        return extraJarsHandler.getMavenDeps();
    }

    private Set<String> getBundleJars (Set<TopologyComponentBundle> bundlesToDeploy) throws IOException {
        Set<String> bundleJars = new HashSet<>();
        for (TopologyComponentBundle topologyComponentBundle: bundlesToDeploy) {
            bundleJars.add(topologyComponentBundle.getBundleJar());
        }
        return bundleJars;
    }

    private void downloadAndCopyJars (Set<String> jarsToDownload, Path destinationPath) throws IOException {
        Set<String> copiedJars = new HashSet<>();
        for (String jar: jarsToDownload) {
            if (!copiedJars.contains(jar)) {
                Path jarPath = Paths.get(jar);
                if (destinationPath == null || jarPath == null) {
                    throw new IllegalArgumentException("null destinationPath or jarPath");
                }
                Path jarFileName = jarPath.getFileName();
                if (jarFileName == null) {
                    throw new IllegalArgumentException("null farFileName");
                }
                File destPath = Paths.get(destinationPath.toString(), jarFileName.toString()).toFile();
                try (InputStream src = fileStorage.download(jar);
                     FileOutputStream dest = new FileOutputStream(destPath)
                ) {
                    IOUtils.copy(src, dest);
                    copiedJars.add(jar);
                    LOG.debug("Jar {} copied to {}", jar, destPath);
                }
            }
        }
    }

    public void setUpClusterArtifacts(Topology topology, TopologyActions topologyActions) throws IOException {
        Namespace namespace = environmentService.getNamespace(topology.getNamespaceId());
        if (namespace == null) {
            throw new RuntimeException("Corresponding namespace not found: " + topology.getNamespaceId());
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Path artifactsDir = topologyActions.getArtifactsLocation(CatalogToLayoutConverter.getTopologyLayout(topology));
        makeEmptyDir(artifactsDir);

        Collection<NamespaceServiceClusterMap> serviceClusterMappings = environmentService.listServiceClusterMapping(namespace.getId());
        for (NamespaceServiceClusterMap serviceClusterMapping : serviceClusterMappings) {
            Service service = environmentService.getServiceByName(serviceClusterMapping.getClusterId(),
                    serviceClusterMapping.getServiceName());
            if (service != null) {
                Collection<ServiceConfiguration> serviceConfigurations = environmentService.listServiceConfigurations(service.getId());
                if (serviceConfigurations != null) {
                    for (ServiceConfiguration serviceConfiguration : serviceConfigurations) {
                        writeConfigurationFile(objectMapper, artifactsDir, serviceConfiguration);
                    }
                }
            }
        }
    }

    private void makeEmptyDir(Path path) throws IOException {
        if (path.toFile().exists()) {
            if (path.toFile().isDirectory()) {
                FileUtils.cleanDirectory(path.toFile());
            } else {
                final String errorMessage = String.format("Location '%s' must be a directory.", path);
                LOG.error(errorMessage);
                throw new IOException(errorMessage);
            }
        } else if (!path.toFile().mkdirs()) {
            LOG.error("Could not create dir {}", path);
            throw new IOException("Could not create dir: " + path);
        }
    }

    // Only known configuration files will be saved to local
    private void writeConfigurationFile(ObjectMapper objectMapper, Path artifactsDir,
                                        ServiceConfiguration configuration) throws IOException {
        String filename = configuration.getFilename();
        if (filename != null && !filename.isEmpty()) {
            // Configuration itself is aware of file name
            ConfigFileType fileType = ConfigFileType.getFileTypeFromFileName(filename);

            if (fileType != null) {
                File destPath = Paths.get(artifactsDir.toString(), filename).toFile();

                Map<String, String> conf = objectMapper.readValue(configuration.getConfiguration(),
                        new TypeReference<Map<String, String>>() {});

                try {
                    configFileWriter.writeConfigToFile(fileType, conf, destPath);
                    LOG.debug("Resource {} written to {}", filename, destPath);
                } catch (IllegalArgumentException e) {
                    LOG.warn("Don't know how to write resource {} skipping...", filename);
                }
            }
        }
    }

    public TopologyActions getTopologyActionsInstance(Topology ds) {
        Namespace namespace = environmentService.getNamespace(ds.getNamespaceId());
        if (namespace == null) {
            throw new RuntimeException("Corresponding namespace not found: " + ds.getNamespaceId());
        }

        TopologyActions topologyActions = topologyActionsContainer.findInstance(namespace);
        if (topologyActions == null) {
            throw new RuntimeException("Can't find Topology Actions for such namespace " + ds.getNamespaceId());
        }
        return topologyActions;
    }

    public TopologyDagBuilder getTopologyDagBuilder() {
        return topologyDagBuilder;
    }

    private TopologyContext getTopologyContext(Topology topology, String asUser) {
        TopologyState state = catalogService
                .getTopologyState(topology.getId())
                .map(s -> stateFactory.getTopologyState(s.getName()))
                .orElse(TOPOLOGY_STATE_INITIAL);
        return new TopologyContext(topology, this, state, asUser);
    }

    public StreamCatalogService getCatalogService() {
        return catalogService;
    }
}
