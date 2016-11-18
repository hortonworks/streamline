package org.apache.streamline.streams.actions.topology.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.streamline.common.util.FileStorage;
import org.apache.streamline.common.util.JsonSchemaValidator;
import org.apache.streamline.streams.catalog.Service;
import org.apache.streamline.streams.catalog.ServiceConfiguration;
import org.apache.streamline.streams.catalog.Topology;
import org.apache.streamline.streams.catalog.configuration.ConfigFileType;
import org.apache.streamline.streams.catalog.configuration.ConfigFileWriter;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.streams.catalog.topology.TopologyComponentBundle;
import org.apache.streamline.streams.catalog.topology.TopologyLayoutValidator;
import org.apache.streamline.streams.catalog.topology.component.TopologyDagBuilder;
import org.apache.streamline.streams.catalog.CatalogToLayoutConverter;
import org.apache.streamline.streams.layout.TopologyLayoutConstants;
import org.apache.streamline.streams.actions.TopologyActions;
import org.apache.streamline.streams.layout.component.TopologyDag;
import org.apache.streamline.streams.layout.component.TopologyLayout;
import org.apache.streamline.streams.layout.exception.ComponentConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TopologyActionsService {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyActionsService.class);

    private final TopologyActions topologyActions;
    private final StreamCatalogService catalogService;
    private final FileStorage fileStorage;
    private final ConfigFileWriter configFileWriter;
    private final TopologyDagBuilder topologyDagBuilder;

    public TopologyActionsService(TopologyActions topologyActions, StreamCatalogService catalogService, FileStorage fileStorage) {
        this.topologyActions = topologyActions;
        this.catalogService = catalogService;
        this.fileStorage = fileStorage;
        this.configFileWriter = new ConfigFileWriter();
        this.topologyDagBuilder = new TopologyDagBuilder(catalogService);
    }

    public void killTopology(TopologyLayout topology) throws Exception {
        topologyActions.kill(topology);
    }

    public void suspendTopology(TopologyLayout topology) throws Exception {
        topologyActions.suspend(topology);
    }

    public void resumeTopology(TopologyLayout topology) throws Exception {
        topologyActions.resume(topology);
    }

    public TopologyActions.Status topologyStatus(TopologyLayout topology) throws Exception {
        return this.topologyActions.status(topology);
    }

    public Topology validateTopology(URL schema, Long topologyId)
            throws Exception {
        Topology result = catalogService.getTopology(topologyId);
        boolean isValidAsPerSchema;
        if (result != null) {
            String json = result.getConfig();
            // first step is to validate against json schema provided
            isValidAsPerSchema = JsonSchemaValidator
                    .isValidJsonAsPerSchema(schema, json);

            if (!isValidAsPerSchema) {
                throw new ComponentConfigException("Topology with id "
                        + topologyId + " failed to validate against json "
                        + "schema");
            }
            // if first step succeeds, proceed to other validations that
            // cannot be covered using json schema
            TopologyLayoutValidator validator = new TopologyLayoutValidator(json);
            validator.validate();

            // finally pass it on for streaming engine based config validations
            this.topologyActions.validate(CatalogToLayoutConverter.getTopologyLayout(result));
        }
        return result;
    }

    public void deployTopology(Topology topology) throws Exception {
        TopologyDag dag = topologyDagBuilder.getDag(topology);
        LOG.debug("Deploying topology {}", topology);
        setUpClusterArtifacts(topology);
        setUpExtraJars(topology);
        topologyActions.deploy(CatalogToLayoutConverter.getTopologyLayout(topology));
    }

    private void setUpExtraJars(Topology topology) throws IOException {
        StormTopologyExtraJarsHandler extraJarsHandler = new StormTopologyExtraJarsHandler(catalogService);
        topology.getTopologyDag().traverse(extraJarsHandler);
        Path extraJarsLocation = topologyActions.getExtraJarsLocation(CatalogToLayoutConverter.getTopologyLayout(topology));
        makeEmptyDir(extraJarsLocation);
        Set<String> extraJars = new HashSet<>();
        extraJars.addAll(extraJarsHandler.getExtraJars());
        extraJars.addAll(getBundleJars(CatalogToLayoutConverter.getTopologyLayout(topology)));
        downloadAndCopyJars(extraJars, extraJarsLocation);
    }

    private Set<String> getBundleJars (TopologyLayout topologyLayout) throws IOException {
        TopologyComponentBundleJarHandler topologyComponentBundleJarHandler = new TopologyComponentBundleJarHandler(catalogService);
        topologyLayout.getTopologyDag().traverse(topologyComponentBundleJarHandler);
        Set<TopologyComponentBundle> bundlesToDeploy = topologyComponentBundleJarHandler.getTopologyComponentBundleSet();
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
                File destPath = Paths.get(destinationPath.toString(), Paths.get(jar).getFileName().toString()).toFile();
                try (InputStream src = fileStorage.downloadFile(jar);
                     FileOutputStream dest = new FileOutputStream(destPath)
                ) {
                    IOUtils.copy(src, dest);
                    copiedJars.add(jar);
                    LOG.debug("Jar {} copied to {}", jar, destPath);
                }
            }
        }
    }

    private void setUpClusterArtifacts(Topology topology) throws IOException {
        String config = topology.getConfig();
        ObjectMapper objectMapper = new ObjectMapper();
        Map jsonMap = objectMapper.readValue(config, Map.class);
        Path artifactsDir = topologyActions.getArtifactsLocation(CatalogToLayoutConverter.getTopologyLayout(topology));
        makeEmptyDir(artifactsDir);
        if (jsonMap != null) {
            List<Object> serviceList = (List<Object>) jsonMap.get(TopologyLayoutConstants.JSON_KEY_SERVICES);
            if (serviceList != null) {
                List<Service> services = objectMapper.readValue(objectMapper.writeValueAsString(serviceList),
                        new TypeReference<List<Service>>() {
                        });

                for (Service service : services) {
                    Collection<ServiceConfiguration> configurations = catalogService.listServiceConfigurations(service.getId());

                    for (ServiceConfiguration configuration : configurations) {
                        writeConfigurationFile(objectMapper, artifactsDir, configuration);
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
                final String errorMessage = String
                        .format("Location '%s' must be a directory.", path);
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

                Map<String, Object> conf = objectMapper.readValue(configuration.getConfiguration(), Map.class);

                try {
                    configFileWriter.writeConfigToFile(fileType, conf, destPath);
                    LOG.debug("Resource {} written to {}", filename, destPath);
                } catch (IllegalArgumentException e) {
                    LOG.warn("Don't know how to write resource {} skipping...", filename);
                }
            }
        }
    }

}
