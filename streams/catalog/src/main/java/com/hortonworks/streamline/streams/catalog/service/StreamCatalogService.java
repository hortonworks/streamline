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

package com.hortonworks.streamline.streams.catalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.hortonworks.streamline.common.ComponentTypes;
import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.registries.common.Schema;
import com.hortonworks.streamline.common.util.FileStorage;
import com.hortonworks.streamline.common.util.ProxyUtil;
import com.hortonworks.streamline.common.util.Utils;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.registries.model.client.MLModelRegistryClient;
import com.hortonworks.streamline.storage.StorableKey;
import com.hortonworks.streamline.storage.StorageManager;
import com.hortonworks.streamline.storage.exception.StorageException;
import com.hortonworks.streamline.storage.util.StorageUtils;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.catalog.TopologyBranchRule;
import com.hortonworks.streamline.streams.catalog.File;
import com.hortonworks.streamline.streams.catalog.Notifier;
import com.hortonworks.streamline.streams.catalog.Projection;
import com.hortonworks.streamline.streams.catalog.TopologyRule;
import com.hortonworks.streamline.streams.catalog.TopologyStream;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.TopologyComponent;
import com.hortonworks.streamline.streams.catalog.TopologyEdge;
import com.hortonworks.streamline.streams.catalog.TopologyEditorMetadata;
import com.hortonworks.streamline.streams.catalog.TopologyOutputComponent;
import com.hortonworks.streamline.streams.catalog.TopologyProcessor;
import com.hortonworks.streamline.streams.catalog.TopologyProcessorStreamMapping;
import com.hortonworks.streamline.streams.catalog.TopologySink;
import com.hortonworks.streamline.streams.catalog.TopologySource;
import com.hortonworks.streamline.streams.catalog.TopologySourceStreamMapping;
import com.hortonworks.streamline.streams.catalog.TopologyVersion;
import com.hortonworks.streamline.streams.catalog.UDF;
import com.hortonworks.streamline.streams.catalog.TopologyWindow;
import com.hortonworks.streamline.streams.catalog.processor.CustomProcessorInfo;
import com.hortonworks.streamline.streams.catalog.rule.RuleParser;
import com.hortonworks.streamline.streams.catalog.topology.TopologyComponentBundle;
import com.hortonworks.streamline.streams.catalog.topology.TopologyComponentUISpecification;
import com.hortonworks.streamline.streams.catalog.topology.TopologyData;
import com.hortonworks.streamline.streams.catalog.topology.component.TopologyDagBuilder;
import com.hortonworks.streamline.streams.catalog.topology.component.TopologyExportVisitor;
import com.hortonworks.streamline.streams.catalog.topology.state.TopologyState;
import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.layout.component.Stream;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.layout.component.rule.Rule;
import com.hortonworks.streamline.streams.layout.exception.ComponentConfigException;
import com.hortonworks.streamline.streams.layout.storm.FluxComponent;
import com.hortonworks.streamline.streams.rule.UDAF;
import com.hortonworks.streamline.streams.rule.UDAF2;
import com.hortonworks.streamline.streams.rule.UDF2;
import com.hortonworks.streamline.streams.rule.UDF3;
import com.hortonworks.streamline.streams.rule.UDF4;
import com.hortonworks.streamline.streams.rule.UDF5;
import com.hortonworks.streamline.streams.rule.UDF6;
import com.hortonworks.streamline.streams.rule.UDF7;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.hortonworks.streamline.common.util.WSUtils.CURRENT_VERSION;
import static com.hortonworks.streamline.common.util.WSUtils.buildEdgesFromQueryParam;
import static com.hortonworks.streamline.common.util.WSUtils.buildEdgesToQueryParam;
import static com.hortonworks.streamline.common.util.WSUtils.currentVersionQueryParam;
import static com.hortonworks.streamline.common.util.WSUtils.versionIdQueryParam;
import static com.hortonworks.streamline.streams.catalog.TopologyEdge.StreamGrouping;
import static com.hortonworks.streamline.streams.catalog.TopologyEditorMetadata.TopologyUIData;

/**
 * A service layer where we could put our business logic.
 */
public class StreamCatalogService {

    private static final Logger LOG = LoggerFactory.getLogger(StreamCatalogService.class);

    // TODO: the namespace and Id generation logic should be moved inside DAO
    private static final String NOTIFIER_INFO_NAMESPACE = new Notifier().getNameSpace();
    private static final String TOPOLOGY_NAMESPACE = new Topology().getNameSpace();
    private static final String TOPOLOGY_VERSIONINFO_NAMESPACE = new TopologyVersion().getNameSpace();
    private static final String STREAMINFO_NAMESPACE = new TopologyStream().getNameSpace();
    private static final String TOPOLOGY_COMPONENT_NAMESPACE = new TopologyComponent().getNameSpace();
    private static final String TOPOLOGY_SOURCE_NAMESPACE = new TopologySource().getNameSpace();
    private static final String TOPOLOGY_SOURCE_STREAM_MAPPING_NAMESPACE = new TopologySourceStreamMapping().getNameSpace();
    private static final String TOPOLOGY_SINK_NAMESPACE = new TopologySink().getNameSpace();
    private static final String TOPOLOGY_PROCESSOR_NAMESPACE = new TopologyProcessor().getNameSpace();
    private static final String TOPOLOGY_PROCESSOR_STREAM_MAPPING_NAMESPACE = new TopologyProcessorStreamMapping().getNameSpace();
    private static final String TOPOLOGY_EDGE_NAMESPACE = new TopologyEdge().getNameSpace();
    private static final String TOPOLOGY_RULEINFO_NAMESPACE = new TopologyRule().getNameSpace();
    private static final String TOPOLOGY_BRANCHRULEINFO_NAMESPACE = new TopologyBranchRule().getNameSpace();
    private static final String TOPOLOGY_WINDOWINFO_NAMESPACE = new TopologyWindow().getNameSpace();
    private static final String UDF_NAMESPACE = new UDF().getNameSpace();
    private static final String TOPOLOGY_STATE_NAMESPACE = new TopologyState().getNameSpace();

    private static final ArrayList<Class<?>> UDF_CLASSES = Lists.newArrayList(UDAF.class, UDAF2.class, com.hortonworks.streamline.streams.rule.UDF.class, UDF2.class,
                                                                              UDF3.class, UDF4.class, UDF5.class, UDF6.class, UDF7.class);
    public static final long PLACEHOLDER_ID = -1L;
    private static final String CLONE_SUFFIX = "-clone";

    private final StorageManager dao;
    private final FileStorage fileStorage;
    private final TopologyDagBuilder topologyDagBuilder;

    public StreamCatalogService(StorageManager dao, FileStorage fileStorage, MLModelRegistryClient modelRegistryClient) {
        this.dao = dao;
        this.fileStorage = fileStorage;
        this.topologyDagBuilder = new TopologyDagBuilder(this, modelRegistryClient);
    }

    public Notifier addNotifierInfo(Notifier notifier) {
        if (notifier.getId() == null) {
            notifier.setId(this.dao.nextId(NOTIFIER_INFO_NAMESPACE));
        }
        if (notifier.getTimestamp() == null) {
            notifier.setTimestamp(System.currentTimeMillis());
        }
        if (StringUtils.isEmpty(notifier.getName())) {
            throw new StorageException("Notifier name empty");
        }
        this.dao.add(notifier);
        return notifier;
    }

    public Notifier getNotifierInfo(Long id) {
        Notifier notifier = new Notifier();
        notifier.setId(id);
        return this.dao.get(new StorableKey(NOTIFIER_INFO_NAMESPACE, notifier.getPrimaryKey()));
    }

    public Collection<Notifier> listNotifierInfos() {
        return this.dao.list(NOTIFIER_INFO_NAMESPACE);
    }

    public Collection<Notifier> listNotifierInfos(List<QueryParam> params) {
        return dao.find(NOTIFIER_INFO_NAMESPACE, params);
    }


    public Notifier removeNotifierInfo(Long notifierId) {
        Notifier notifier = new Notifier();
        notifier.setId(notifierId);
        return dao.remove(new StorableKey(NOTIFIER_INFO_NAMESPACE, notifier.getPrimaryKey()));
    }


    public Notifier addOrUpdateNotifierInfo(Long id, Notifier notifier) {
        notifier.setId(id);
        notifier.setTimestamp(System.currentTimeMillis());
        this.dao.addOrUpdate(notifier);
        return notifier;
    }

    public Collection<TopologyVersion> listCurrentTopologyVersionInfos() {
        return listTopologyVersionInfos(currentVersionQueryParam());
    }

    public Collection<TopologyVersion> listTopologyVersionInfos(List<QueryParam> queryParams) {
        return dao.find(TOPOLOGY_VERSIONINFO_NAMESPACE, queryParams);
    }


    public Optional<TopologyVersion> getCurrentTopologyVersionInfo(Long topologyId) {
        Collection<TopologyVersion> versions = listTopologyVersionInfos(
                WSUtils.currentTopologyVersionQueryParam(topologyId, null));
        if (versions.isEmpty()) {
            LOG.warn("No current version for topology " + topologyId);
            return Optional.empty();
        } else if (versions.size() > 1) {
            throw new IllegalStateException("More than one 'CURRENT' version for topology id: " + topologyId);
        }
        return Optional.of(versions.iterator().next());
    }

    // latest version before the CURRENT version
    public Optional<TopologyVersion> getLatestVersionInfo(Long topologyId) {
        Collection<TopologyVersion> versions =
                listTopologyVersionInfos(WSUtils.buildTopologyIdAwareQueryParams(topologyId, null));
        return  versions.stream()
                .filter(v -> !v.getName().equals(CURRENT_VERSION))
                .max((versionInfo1, versionInfo2) -> {
                    // compares the number part from version strings like V1, V2 ...
                    return versionInfo1.getVersionNumber() - versionInfo2.getVersionNumber();
                });
    }
    public TopologyVersion getTopologyVersionInfo(Long versionId) {
        TopologyVersion topologyVersion = new TopologyVersion();
        topologyVersion.setId(versionId);
        return dao.get(topologyVersion.getStorableKey());
    }

    public TopologyVersion addTopologyVersionInfo(TopologyVersion topologyVersion) {
        if (topologyVersion.getId() == null) {
            topologyVersion.setId(this.dao.nextId(TOPOLOGY_VERSIONINFO_NAMESPACE));
        }
        if (topologyVersion.getTimestamp() == null) {
            topologyVersion.setTimestamp(System.currentTimeMillis());
        }
        dao.add(topologyVersion);
        return topologyVersion;
    }

    public TopologyVersion addOrUpdateTopologyVersionInfo(Long versionId, TopologyVersion topologyVersion) {
        topologyVersion.setId(versionId);
        topologyVersion.setTimestamp(System.currentTimeMillis());
        this.dao.addOrUpdate(topologyVersion);
        return topologyVersion;
    }

    public Long getVersionTimestamp(Long versionId) {
        TopologyVersion versionInfo = getTopologyVersionInfo(versionId);
        if (versionInfo == null) {
            throw new IllegalArgumentException("No version with versionId " + versionId);
        }
        return versionInfo.getTimestamp();
    }

    public TopologyVersion updateVersionTimestamp(Long versionId) {
        return updateVersionTimestamp(versionId, System.currentTimeMillis());
    }

    public TopologyVersion updateVersionTimestamp(Long versionId, Long timestamp) {
        TopologyVersion topologyVersion = getTopologyVersionInfo(versionId);
        if (topologyVersion == null) {
            throw new IllegalStateException("No version with version Id " + versionId);
        }
        topologyVersion.setTimestamp(timestamp);
        dao.addOrUpdate(topologyVersion);
        return topologyVersion;
    }

    public TopologyVersion removeTopologyVersionInfo(Long versionId) {
        TopologyVersion topologyVersion = new TopologyVersion();
        topologyVersion.setId(versionId);
        return dao.remove(new StorableKey(TOPOLOGY_VERSIONINFO_NAMESPACE, topologyVersion.getPrimaryKey()));
    }

    /**
     * Lists the 'CURRENT' version of topologies
     */
    public Collection<Topology> listTopologies() {
        List<Topology> topologies = new ArrayList<>();
        for (TopologyVersion version: listCurrentTopologyVersionInfos()) {
            topologies.addAll(listTopologies(version.getId()));
        }
        return topologies;
    }

    private Collection<Topology> listTopologies(Long versionId) {
        Collection<Topology> topologies = this.dao.find(TOPOLOGY_NAMESPACE, versionIdQueryParam(versionId));
        Long versionTimestamp = getVersionTimestamp(versionId);
        topologies.forEach(x -> x.setVersionTimestamp(versionTimestamp));
        return topologies;
    }

    public Collection<Topology> listTopologies(List<QueryParam> queryParams) {
        Collection<Topology> topologies = this.dao.find(TOPOLOGY_NAMESPACE, queryParams);
        topologies.forEach(t -> t.setVersionTimestamp(getVersionTimestamp(t.getVersionId())));
        return topologies;
    }

    public Long getCurrentVersionId(Long topologyId) {
        Optional<TopologyVersion> versionInfo = getCurrentTopologyVersionInfo(topologyId);
        return versionInfo.isPresent() ? versionInfo.get().getId() : -1L;
    }

    /**
     * returns the 'CURRENT' version of the topology with given topologyId
     */
    public Topology getTopology(Long topologyId) {
        return getTopology(topologyId, getCurrentVersionId(topologyId));
    }

    public Topology getTopology(Long topologyId, Long versionId) {
        Topology topology = new Topology();
        topology.setId(topologyId);
        topology.setVersionId(versionId);
        Topology result = this.dao.get(topology.getStorableKey());
        if (result != null) {
            result.setVersionTimestamp(getVersionTimestamp(versionId));
        }
        return result;
    }

    public Topology addTopology(Topology topology) {
        if (topology.getId() == null) {
            topology.setId(this.dao.nextId(TOPOLOGY_NAMESPACE));
        }
        validateTopology(topology);
        topology.setVersionId(PLACEHOLDER_ID);
        this.dao.add(topology);
        LOG.debug("Added topology {}", topology);
        long timestamp = System.currentTimeMillis();
        topology.setVersionTimestamp(timestamp);
        TopologyVersion versionInfo = addCurrentTopologyVersionInfo(topology.getId(), timestamp);
        LOG.debug("Added version info {}", versionInfo);

        // remove topology with placeholder version first
        // WARN: don't use removeTopology since it also removes PLACEHOLDER topology version info!
        removeOnlyTopologyEntity(topology.getId(), topology.getVersionId());

        // put actual version id
        topology.setVersionId(versionInfo.getId());

        this.dao.add(topology);
        return topology;
    }

    private Topology removeOnlyTopologyEntity(Long topologyId, Long versionId) {
        Topology topologyForDelete = new Topology();
        topologyForDelete.setId(topologyId);
        topologyForDelete.setVersionId(versionId);
        return dao.remove(topologyForDelete.getStorableKey());
    }

    // create a 'CURRENT' version for given topology id
    private TopologyVersion addCurrentTopologyVersionInfo(Long topologyId, Long timestamp) {
        TopologyVersion versionInfo = new TopologyVersion();
        versionInfo.setName(CURRENT_VERSION);
        versionInfo.setDescription("");
        versionInfo.setTimestamp(timestamp);
        versionInfo.setTopologyId(topologyId);
        return addTopologyVersionInfo(versionInfo);
    }

    /**
     * removes the 'CURRENT' version of the topology with the given id.
     */
    public Topology removeTopology(Long topologyId, boolean recurse) {
        return removeTopology(topologyId, getCurrentVersionId(topologyId), recurse);
    }

    public Topology removeTopology(Long topologyId, Long versionId, boolean recurse) {
        Topology topology = new Topology();
        topology.setId(topologyId);
        topology.setVersionId(versionId);
        if (recurse) {
            try {
                removeTopologyDependencies(topology.getId(), topology.getVersionId());
            } catch (Exception ex) {
                LOG.error("Got exception while removing topology dependencies", ex);
                throw new RuntimeException(ex);
            }
        }
        Topology removedTopology = dao.remove(topology.getStorableKey());
        removeTopologyVersionInfo(versionId);
        return removedTopology;
    }

    private void removeTopologyDependencies(Long topologyId, Long versionId) throws Exception {
        List<QueryParam> topologyIdVersionIdQueryParams = WSUtils.buildTopologyIdAndVersionIdAwareQueryParams(
                topologyId, versionId, null);
        // remove edges
        Collection<TopologyEdge> edges = listTopologyEdges(topologyIdVersionIdQueryParams);
        for (TopologyEdge edge: edges) {
            removeTopologyEdge(topologyId, edge.getId(), versionId);
        }

        // remove rules
        Collection<TopologyRule> topologyRules = listRules(topologyIdVersionIdQueryParams);
        for (TopologyRule topologyRule : topologyRules) {
            removeRule(topologyId, topologyRule.getId(), versionId);
        }

        // remove windowed rules
        Collection<TopologyWindow> topologyWindows = listWindows(topologyIdVersionIdQueryParams);
        for (TopologyWindow topologyWindow : topologyWindows) {
            removeWindow(topologyId, topologyWindow.getId(), versionId);
        }

        // remove branch rules
        Collection<TopologyBranchRule> topologyBranchRules = listBranchRules(topologyIdVersionIdQueryParams);
        for (TopologyBranchRule topologyBranchRule : topologyBranchRules) {
            removeBranchRule(topologyId, topologyBranchRule.getId(), versionId);
        }

        // remove sinks
        Collection<TopologySink> sinks = listTopologySinks(topologyIdVersionIdQueryParams);
        for (TopologySink sink : sinks) {
            removeTopologySink(topologyId, sink.getId(), versionId, false);
        }

        // remove processors
        Collection<TopologyProcessor> processors = listTopologyProcessors(topologyIdVersionIdQueryParams);
        for (TopologyProcessor processor: processors) {
            removeTopologyProcessor(topologyId, processor.getId(), versionId, false);
        }

        // remove sources
        Collection<TopologySource> sources = listTopologySources(topologyIdVersionIdQueryParams);
        for (TopologySource source : sources) {
            removeTopologySource(topologyId, source.getId(), versionId, false);
        }

        // remove output streams
        Collection<TopologyStream> topologyStreams = listStreamInfos(topologyIdVersionIdQueryParams);
        for (TopologyStream topologyStream : topologyStreams) {
            removeStreamInfo(topologyId, topologyStream.getId(), versionId);
        }

        // remove topology editor metadata
        removeTopologyEditorMetadata(topologyId, versionId);
    }

    /**
     * Clones the given version of the topology and all its dependencies to a new 'CURRENT' version.
     * The ids of the topology and its dependencies are retained.
     */
    public Topology cloneTopologyVersion(Long topologyId, Long versionId) {
        Topology topology = getTopology(topologyId, versionId);
        if (topology != null) {
            try {
                topology = addTopology(new Topology(topology));
                copyTopologyDependencies(topologyId, versionId, topology.getVersionId());
            } catch (Exception ex) {
                LOG.error("Got exception while copying topology dependencies", ex);
                if (topology != null) {
                    removeTopology(topology.getId(), topology.getVersionId(), true);
                }
                throw new RuntimeException(ex);
            }
        }
        return topology;
    }

    private void copyTopologyDependencies(Long topologyId, Long oldVersionId, Long newVersionId) throws Exception {
        List<QueryParam> topologyIdVersionIdQueryParams = WSUtils.buildTopologyIdAndVersionIdAwareQueryParams(
                topologyId, oldVersionId, null);

        // topology editor metadata
        TopologyEditorMetadata metadata = getTopologyEditorMetadata(topologyId, oldVersionId);
        if (metadata != null) {
            addTopologyEditorMetadata(topologyId, newVersionId, new TopologyEditorMetadata(metadata));
        }

        // sources, output streams
        Collection<TopologySource> sources = listTopologySources(topologyIdVersionIdQueryParams);
        for (TopologySource source : sources) {
            addTopologySource(topologyId, newVersionId, new TopologySource(source));
        }

        // processors, output streams
        Collection<TopologyProcessor> processors = listTopologyProcessors(topologyIdVersionIdQueryParams);
        for (TopologyProcessor processor: processors) {
            addTopologyProcessor(topologyId, newVersionId, new TopologyProcessor(processor));
        }

        // add sinks
        Collection<TopologySink> sinks = listTopologySinks(topologyIdVersionIdQueryParams);
        for (TopologySink sink : sinks) {
            addTopologySink(topologyId, newVersionId, new TopologySink(sink));
        }

        // branch rules
        Collection<TopologyBranchRule> topologyBranchRules = listBranchRules(topologyIdVersionIdQueryParams);
        for (TopologyBranchRule topologyBranchRule : topologyBranchRules) {
            addBranchRule(topologyId, newVersionId, new TopologyBranchRule(topologyBranchRule));
        }

        // windowed rules
        Collection<TopologyWindow> topologyWindows = listWindows(topologyIdVersionIdQueryParams);
        for (TopologyWindow topologyWindow : topologyWindows) {
            addWindow(topologyId, newVersionId, new TopologyWindow(topologyWindow));
        }

        // rules
        Collection<TopologyRule> topologyRules = listRules(topologyIdVersionIdQueryParams);
        for (TopologyRule topologyRule : topologyRules) {
            addRule(topologyId, newVersionId, new TopologyRule(topologyRule));
        }

        // add edges
        Collection<TopologyEdge> edges = listTopologyEdges(topologyIdVersionIdQueryParams);
        for (TopologyEdge edge: edges) {
            addTopologyEdge(topologyId, newVersionId, new TopologyEdge(edge));
        }
    }

    public Topology addOrUpdateTopology(Long topologyId, Topology topology) {
        return addOrUpdateTopology(topologyId, getCurrentVersionId(topologyId), topology);
    }

    private Topology addOrUpdateTopology(Long topologyId, Long versionId, Topology topology) {
        topology.setId(topologyId);
        topology.setVersionId(versionId);
        long timestamp = System.currentTimeMillis();
        topology.setVersionTimestamp(timestamp);
        validateTopology(topology);
        this.dao.addOrUpdate(topology);
        updateVersionTimestamp(versionId, timestamp);
        return topology;
    }

    public TopologyComponent getTopologyComponent(Long topologyId, Long topologyComponentId) {
        TopologyComponent topologyComponent = getTopologySource(topologyId, topologyComponentId);
        if (topologyComponent == null) {
            topologyComponent = getTopologyProcessor(topologyId, topologyComponentId);
            if (topologyComponent == null) {
                topologyComponent = getTopologySink(topologyId, topologyComponentId);
            }
        }
        return topologyComponent;
    }

    public String exportTopology(Topology topology) throws Exception {
        Preconditions.checkNotNull(topology);
        TopologyData topologyData = doExportTopology(topology);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(topologyData);
    }

    private TopologyData doExportTopology(Topology topology) throws Exception {
        TopologyDag dag = topologyDagBuilder.getDag(topology);
        topology.setTopologyDag(dag);
        TopologyData topologyData = new TopologyData();
        TopologyExportVisitor exportVisitor = new TopologyExportVisitor(topology.getId(), topologyData, this);
        topologyData.setTopologyName(topology.getName());
        topologyData.setConfig(topology.getConfig());
        TopologyDag topologyDag = topology.getTopologyDag();
        if (topologyDag != null) {
            topologyDag.traverse(exportVisitor);
        }
        topologyData.setMetadata(getTopologyEditorMetadata(topology.getId()));
        return topologyData;
    }

    private List<Long> importOutputStreams(Long newTopologyId, Map<Long, Long> oldToNewStreamIds, List<TopologyStream> streams) {
        List<Long> importedOutputStreamIds = new ArrayList<>();
        for (TopologyStream stream : streams) {
            Long oldId = stream.getId();
            Long newId = oldToNewStreamIds.get(oldId);
            if (newId == null) {
                stream.setId(null);
                TopologyStream addedTopologyStream = addStreamInfo(newTopologyId, stream);
                newId = addedTopologyStream.getId();
                oldToNewStreamIds.put(oldId, newId);
            }
            importedOutputStreamIds.add(newId);
        }
        return importedOutputStreamIds;
    }

    private TopologyComponentBundle getCurrentTopologyComponentBundle(TopologyComponentBundle.TopologyComponentType type, String subType) {
        Collection<TopologyComponentBundle> bundles = listTopologyComponentBundlesForTypeWithFilter(type, Collections.singletonList(
                new QueryParam(TopologyComponentBundle.SUB_TYPE, subType)
        ));
        if (bundles.size() != 1) {
            throw new IllegalStateException("Not able to find topology component bundle for type " + type
            + " sub type " + subType);
        }
        return bundles.iterator().next();
    }

    private Topology doImportTopology(Topology newTopology, TopologyData topologyData) throws Exception {
        List<TopologySource> topologySources = topologyData.getSources();
        Map<Long, Long> oldToNewComponentIds = new HashMap<>();
        Map<Long, Long> oldToNewRuleIds = new HashMap<>();
        Map<Long, Long> oldToNewWindowIds = new HashMap<>();
        Map<Long, Long> oldToNewBranchRuleIds = new HashMap<>();
        Map<Long, Long> oldToNewStreamIds = new HashMap<>();

        // import source streams
        for (TopologySource topologySource : topologySources) {
            topologySource.setOutputStreamIds(importOutputStreams(newTopology.getId(),
                    oldToNewStreamIds, topologySource.getOutputStreams()));
            topologySource.setOutputStreams(null);
        }

        // import processor streams
        for (TopologyProcessor topologyProcessor : topologyData.getProcessors()) {
            topologyProcessor.setOutputStreamIds(importOutputStreams(newTopology.getId(),
                    oldToNewStreamIds, topologyProcessor.getOutputStreams()));
            topologyProcessor.setOutputStreams(null);
        }

        // import rules
        for (TopologyRule rule : topologyData.getRules()) {
            Long currentId = rule.getId();
            rule.setId(null);
            TopologyRule addedRule = addRule(newTopology.getId(), rule);
            oldToNewRuleIds.put(currentId, addedRule.getId());
        }

        // import windowed rules
        for (TopologyWindow window : topologyData.getWindows()) {
            Long currentId = window.getId();
            window.setId(null);
            TopologyWindow addedWindow = addWindow(newTopology.getId(), window);
            oldToNewWindowIds.put(currentId, addedWindow.getId());
        }

        // import branch rules
        for (TopologyBranchRule branchRule : topologyData.getBranchRules()) {
            Long currentId = branchRule.getId();
            branchRule.setId(null);
            TopologyBranchRule addedBranchRule = addBranchRule(newTopology.getId(), branchRule);
            oldToNewBranchRuleIds.put(currentId, addedBranchRule.getId());
        }

        // import sources
        for (TopologySource topologySource : topologySources) {
            Long oldComponentId = topologySource.getId();
            topologySource.setId(null);
            topologySource.setTopologyId(newTopology.getId());
            TopologyComponentBundle bundle = getCurrentTopologyComponentBundle(
                    TopologyComponentBundle.TopologyComponentType.SOURCE,
                    topologyData.getBundleIdToType().get(topologySource.getTopologyComponentBundleId().toString()));
            topologySource.setTopologyComponentBundleId(bundle.getId());
            addTopologySource(newTopology.getId(), topologySource);
            oldToNewComponentIds.put(oldComponentId, topologySource.getId());
        }

        // import processors
        for (TopologyProcessor topologyProcessor : topologyData.getProcessors()) {
            Long oldComponentId = topologyProcessor.getId();
            topologyProcessor.setId(null);
            topologyProcessor.setTopologyId(newTopology.getId());
            TopologyComponentBundle bundle = getCurrentTopologyComponentBundle(
                    TopologyComponentBundle.TopologyComponentType.PROCESSOR,
                    topologyData.getBundleIdToType().get(topologyProcessor.getTopologyComponentBundleId().toString()));
            topologyProcessor.setTopologyComponentBundleId(bundle.getId());
            Optional<Object> ruleListObj = topologyProcessor.getConfig().getAnyOptional(RulesProcessor.CONFIG_KEY_RULES);
            ruleListObj.ifPresent(ruleList -> {
                List<Long> ruleIds = new ObjectMapper().convertValue(ruleList, new TypeReference<List<Long>>() {});
                List<Long> updatedRuleIds = new ArrayList<>();
                if (bundle.getSubType().equals(ComponentTypes.RULE)) {
                    ruleIds.forEach(ruleId -> updatedRuleIds.add(oldToNewRuleIds.get(ruleId)));
                } else if (bundle.getSubType().equals(ComponentTypes.BRANCH)) {
                    ruleIds.forEach(ruleId -> updatedRuleIds.add(oldToNewBranchRuleIds.get(ruleId)));
                } else if (bundle.getSubType().equals(ComponentTypes.WINDOW)) {
                    ruleIds.forEach(ruleId -> updatedRuleIds.add(oldToNewWindowIds.get(ruleId)));
                }
                topologyProcessor.getConfig().setAny(RulesProcessor.CONFIG_KEY_RULES, updatedRuleIds);
            });
            addTopologyProcessor(newTopology.getId(), topologyProcessor);
            oldToNewComponentIds.put(oldComponentId, topologyProcessor.getId());
        }

        // import sinks
        for (TopologySink topologySink : topologyData.getSinks()) {
            topologySink.setTopologyId(newTopology.getId());
            Long currentId = topologySink.getId();
            topologySink.setId(null);
            TopologyComponentBundle bundle = getCurrentTopologyComponentBundle(
                    TopologyComponentBundle.TopologyComponentType.SINK,
                    topologyData.getBundleIdToType().get(topologySink.getTopologyComponentBundleId().toString()));
            topologySink.setTopologyComponentBundleId(bundle.getId());
            addTopologySink(newTopology.getId(), topologySink);
            oldToNewComponentIds.put(currentId, topologySink.getId());
        }

        // import edges
        for (TopologyEdge topologyEdge : topologyData.getEdges()) {
            List<StreamGrouping> streamGroupings = topologyEdge.getStreamGroupings();
            for (StreamGrouping streamGrouping : streamGroupings) {
                Long newStreamId = oldToNewStreamIds.get(streamGrouping.getStreamId());
                streamGrouping.setStreamId(newStreamId);
            }
            topologyEdge.setId(null);
            topologyEdge.setTopologyId(newTopology.getId());
            topologyEdge.setFromId(oldToNewComponentIds.get(topologyEdge.getFromId()));
            topologyEdge.setToId(oldToNewComponentIds.get(topologyEdge.getToId()));
            addTopologyEdge(newTopology.getId(), topologyEdge);
        }

        // import topology editor metadata
        TopologyEditorMetadata topologyEditorMetadata = topologyData.getTopologyEditorMetadata();
        topologyEditorMetadata.setTopologyId(newTopology.getId());
        TopologyUIData topologyUIData = new ObjectMapper().readValue(topologyEditorMetadata.getData(), TopologyUIData.class);
        topologyUIData.getSources().forEach(c -> c.setId(oldToNewComponentIds.get(c.getId())));
        topologyUIData.getProcessors().forEach(c -> c.setId(oldToNewComponentIds.get(c.getId())));
        topologyUIData.getSinks().forEach(c -> c.setId(oldToNewComponentIds.get(c.getId())));
        topologyEditorMetadata.setData(new ObjectMapper().writeValueAsString(topologyUIData));
        addTopologyEditorMetadata(newTopology.getId(), topologyData.getTopologyEditorMetadata());
        return newTopology;
    }

    public Topology importTopology(Long namespaceId, TopologyData topologyData) throws Exception {
        Preconditions.checkNotNull(topologyData);
        Topology newTopology = new Topology();
        try {
            newTopology.setName(topologyData.getTopologyName());
            newTopology.setConfig(topologyData.getConfig());
            newTopology.setNamespaceId(namespaceId);
            addTopology(newTopology);
        } catch (Exception ex) {
            LOG.error("Got exception while importing the topology", ex);
            throw ex;
        }

        try {
            doImportTopology(newTopology, topologyData);
        } catch (Exception ex) {
            LOG.error("Got exception while importing the topology", ex);
            removeTopology(newTopology.getId(), true);
            throw ex;
        }
        return newTopology;
    }

    public Topology cloneTopology(Long namespaceId, Topology topology) throws Exception {
        Preconditions.checkNotNull(topology, "Topology does not exist");
        TopologyData exported = new TopologyData(doExportTopology(topology));
        Optional<String> latest = getLatestCloneName(exported.getTopologyName(), listTopologies());
        exported.setTopologyName(getNextCloneName(latest.orElse(topology.getName())));
        if (namespaceId == null) {
            namespaceId = topology.getNamespaceId();
        }
        return importTopology(namespaceId, exported);
    }

    Optional<String> getLatestCloneName(String topologyName, Collection<Topology> topologies) {
        return Utils.getLatestName(
                topologies.stream().map(Topology::getName).collect(Collectors.toSet()),
                Utils.getPrefix(topologyName, CLONE_SUFFIX),
                CLONE_SUFFIX);
    }

    String getNextCloneName(String topologyName) {
        return Utils.getNextName(topologyName, CLONE_SUFFIX);
    }

    public Optional<TopologyState> getTopologyState(Long topologyId) {
        TopologyState state = new TopologyState();
        state.setTopologyId(topologyId);
        TopologyState result = this.dao.get(state.getStorableKey());
        return Optional.ofNullable(result);
    }

    public TopologyState addTopologyState(Long topologyId, TopologyState state) {
        state.setTopologyId(topologyId);
        this.dao.add(state);
        return state;
    }

    public TopologyState addOrUpdateTopologyState(Long topologyID, TopologyState state) {
        state.setTopologyId(topologyID);
        dao.addOrUpdate(state);
        return state;
    }

    public TopologyState removeTopologyState(Long topologyId) {
        TopologyState state = new TopologyState();
        state.setTopologyId(topologyId);
        return dao.remove(new StorableKey(TOPOLOGY_STATE_NAMESPACE, state.getPrimaryKey()));
    }

    public Collection<TopologyComponentBundle.TopologyComponentType> listTopologyComponentBundleTypes() {
        return Arrays.asList(TopologyComponentBundle.TopologyComponentType.values());
    }

    public Collection<TopologyComponentBundle> listTopologyComponentBundlesForTypeWithFilter(TopologyComponentBundle.TopologyComponentType componentType,
                                                                                  List<QueryParam> params) {
        List<TopologyComponentBundle> topologyComponentBundles = new ArrayList<>();
        String ns = TopologyComponentBundle.NAME_SPACE;
        Collection<TopologyComponentBundle> filtered = dao.find(ns, params);
        for (TopologyComponentBundle tcb : filtered) {
            if (tcb.getType().equals(componentType)) {
                topologyComponentBundles.add(tcb);
            }
        }
        return topologyComponentBundles;
    }

    public TopologyComponentBundle getTopologyComponentBundle(Long topologyComponentBundleId) {
        TopologyComponentBundle topologyComponentBundle = new TopologyComponentBundle();
        topologyComponentBundle.setId(topologyComponentBundleId);
        return this.dao.get(topologyComponentBundle.getStorableKey());
    }

    public TopologyComponentBundle addTopologyComponentBundle (TopologyComponentBundle topologyComponentBundle, InputStream bundleJar) throws
            ComponentConfigException, IOException {
        topologyComponentBundle.getTopologyComponentUISpecification().validate();
        loadTransformationClassForBundle(topologyComponentBundle, bundleJar);
        if (!topologyComponentBundle.getBuiltin()) {
            topologyComponentBundle.setBundleJar(getTopologyComponentBundleJarName(topologyComponentBundle));
            uploadFileToStorage(bundleJar, topologyComponentBundle.getBundleJar());
        }
        try {
            if (topologyComponentBundle.getId() == null) {
                topologyComponentBundle.setId(this.dao.nextId(TopologyComponentBundle.NAME_SPACE));
            }
            if (topologyComponentBundle.getTimestamp() == null) {
                topologyComponentBundle.setTimestamp(System.currentTimeMillis());
            }
            this.dao.add(topologyComponentBundle);
        } catch (StorageException e) {
            if (!topologyComponentBundle.getBuiltin()) {
                LOG.debug("StorageException while adding the bundle. Deleting the bundle jar.");
                deleteFileFromStorage(topologyComponentBundle.getBundleJar());
            }
            throw e;
        }
        return topologyComponentBundle;
    }

    public TopologyComponentBundle addOrUpdateTopologyComponentBundle (Long id, TopologyComponentBundle topologyComponentBundle, InputStream bundleJar) throws
            ComponentConfigException, IOException {
        topologyComponentBundle.getTopologyComponentUISpecification().validate();
        loadTransformationClassForBundle(topologyComponentBundle, bundleJar);
        if (!topologyComponentBundle.getBuiltin()) {
            topologyComponentBundle.setBundleJar(getTopologyComponentBundleJarName(topologyComponentBundle));
            uploadFileToStorage(bundleJar, topologyComponentBundle.getBundleJar());
        }
        TopologyComponentBundle existing = new TopologyComponentBundle();
        existing.setId(id);
        existing = this.dao.get(existing.getStorableKey());
        if (!existing.getBuiltin()) {
            try {
                deleteFileFromStorage(existing.getBundleJar());
            } catch (IOException e) {
                if (!topologyComponentBundle.getBuiltin()) {
                    deleteFileFromStorage(topologyComponentBundle.getBundleJar());
                }
                throw e;
            }
        }
        try {
            topologyComponentBundle.setId(id);
            topologyComponentBundle.setTimestamp(System.currentTimeMillis());
            this.dao.addOrUpdate(topologyComponentBundle);
        } catch (StorageException e) {
            if (!topologyComponentBundle.getBuiltin()) {
                deleteFileFromStorage(topologyComponentBundle.getBundleJar());
            }
            throw e;
        }
        return topologyComponentBundle;
    }

    public TopologyComponentBundle removeTopologyComponentBundle (Long id) throws IOException {
        TopologyComponentBundle topologyComponentBundle = new TopologyComponentBundle();
        topologyComponentBundle.setId(id);
        TopologyComponentBundle existing = this.dao.get(topologyComponentBundle.getStorableKey());
        if (!existing.getBuiltin()) {
            deleteFileFromStorage(existing.getBundleJar());
        }
        return dao.remove(existing.getStorableKey());
    }

    public InputStream getFileFromJarStorage(String fileName) throws IOException {
        return this.fileStorage.downloadFile(fileName);
    }

    public Collection<CustomProcessorInfo> listCustomProcessorsFromBundleWithFilter(List<QueryParam> params) throws IOException {
        Collection<TopologyComponentBundle> customProcessors = this.listCustomProcessorBundlesWithFilter(params);
        Collection<CustomProcessorInfo> result = new ArrayList<>();
        for (TopologyComponentBundle cp : customProcessors) {
            CustomProcessorInfo customProcessorInfo = new CustomProcessorInfo();
            result.add(customProcessorInfo.fromTopologyComponentBundle(cp));
        }
        return result;
    }

    private Collection<TopologyComponentBundle> listCustomProcessorBundlesWithFilter(List<QueryParam> params) throws IOException {
        List<QueryParam> queryParamsForTopologyComponent = new ArrayList<>();
        queryParamsForTopologyComponent.add(new QueryParam(TopologyComponentBundle.SUB_TYPE, TopologyLayoutConstants.JSON_KEY_CUSTOM_PROCESSOR_SUB_TYPE));
        for (QueryParam qp : params) {
            if (qp.getName().equals(TopologyComponentBundle.STREAMING_ENGINE)) {
                queryParamsForTopologyComponent.add(qp);
            }
        }
        Collection<TopologyComponentBundle> customProcessors = this.listTopologyComponentBundlesForTypeWithFilter(TopologyComponentBundle.TopologyComponentType
                        .PROCESSOR, queryParamsForTopologyComponent);
        Collection<TopologyComponentBundle> result = new ArrayList<>();
        for (TopologyComponentBundle cp : customProcessors) {
            Map<String, Object> config = new HashMap<>();
            for (TopologyComponentUISpecification.UIField uiField: cp.getTopologyComponentUISpecification().getFields()) {
                config.put(uiField.getFieldName(), uiField.getDefaultValue());
            }
            boolean matches = true;
            for (QueryParam qp : params) {
                if (!qp.getName().equals(TopologyComponentBundle.STREAMING_ENGINE) && !qp.getValue().equals(config.get(qp.getName()))) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                result.add(cp);
            }
        }
        return result;
    }

    public CustomProcessorInfo addCustomProcessorInfoAsBundle(CustomProcessorInfo customProcessorInfo, InputStream jarFile) throws IOException,
            ComponentConfigException {
        try {
            uploadFileToStorage(jarFile, customProcessorInfo.getJarFileName());
            TopologyComponentBundle topologyComponentBundle = customProcessorInfo.toTopologyComponentBundle();
            this.addTopologyComponentBundle(topologyComponentBundle, null);
        } catch (IOException e) {
            LOG.error("IOException thrown while trying to upload jar for %s", customProcessorInfo.getName());
            throw e;
        } catch (StorageException se) {
            LOG.error("StorageException thrown while adding custom processor info %s", customProcessorInfo.getName());
            try {
                deleteFileFromStorage(customProcessorInfo.getJarFileName());
            } catch (IOException e) {
                LOG.error("Unexpected exception thrown while cleaning up custom processor info %s", customProcessorInfo.getName());
                throw e;
            }
            throw se;
        }
        return customProcessorInfo;
    }

    public CustomProcessorInfo updateCustomProcessorInfoAsBundle(CustomProcessorInfo customProcessorInfo, InputStream jarFile) throws
            IOException, ComponentConfigException {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam(CustomProcessorInfo.NAME, customProcessorInfo.getName()));
        Collection<TopologyComponentBundle> result = this.listCustomProcessorBundlesWithFilter(queryParams);
        if (result.isEmpty() || result.size() != 1) {
            throw new IOException("Failed to update custom processor with name:" + customProcessorInfo.getName());
        }
        TopologyComponentBundle customProcessorBundle = result.iterator().next();
        deleteFileFromStorage(new CustomProcessorInfo().fromTopologyComponentBundle(customProcessorBundle).getJarFileName());
        uploadFileToStorage(jarFile, customProcessorInfo.getJarFileName());
        TopologyComponentBundle newCustomProcessorBundle = customProcessorInfo.toTopologyComponentBundle();
        this.addOrUpdateTopologyComponentBundle(customProcessorBundle.getId(), newCustomProcessorBundle, null);
        return customProcessorInfo;
    }

    public String uploadFileToStorage(InputStream inputStream, String jarFileName) throws IOException {
        return fileStorage.uploadFile(inputStream, jarFileName);
    }

    public InputStream downloadFileFromStorage(String jarName) throws IOException {
        return fileStorage.downloadFile(jarName);
    }

    public boolean deleteFileFromStorage(String jarName) throws IOException {
        return fileStorage.deleteFile(jarName);
    }

    public CustomProcessorInfo removeCustomProcessorInfoAsBundle(String name) throws IOException {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam(CustomProcessorInfo.NAME, name));
        Collection<TopologyComponentBundle> result = this.listCustomProcessorBundlesWithFilter(queryParams);
        if (result.isEmpty() || result.size() != 1) {
            throw new IOException("Failed to delete custom processor with name:" + name);
        }
        TopologyComponentBundle customProcessorBundle = result.iterator().next();
        Collection<TopologyProcessor> processors = this.listTopologyProcessors();
        if (processors != null && !processors.isEmpty()) {
            for (TopologyProcessor topologyProcessor: processors) {
                if (topologyProcessor.getTopologyComponentBundleId().equals(customProcessorBundle.getId())) {
                    throw new IOException("Cannot delete custom processor as it is being used in one of the topologies.");
                }
            }
        }
        CustomProcessorInfo customProcessorInfo = new CustomProcessorInfo();
        deleteFileFromStorage(customProcessorInfo.fromTopologyComponentBundle(customProcessorBundle).getJarFileName());
        this.removeTopologyComponentBundle(customProcessorBundle.getId());
        return customProcessorInfo;
    }

    public Collection<TopologyEditorMetadata> listTopologyEditorMetadata() {
        List<TopologyEditorMetadata> metadatas = new ArrayList<>();
        Collection<TopologyVersion> currentVersions = listCurrentTopologyVersionInfos();
        for (TopologyVersion version : currentVersions) {
            List<QueryParam> queryParams = WSUtils.buildTopologyIdAndVersionIdAwareQueryParams(
                    version.getTopologyId(), version.getId(), null);
            metadatas.addAll(listTopologyEditorMetadata(queryParams));
        }
        return metadatas;
    }

    public Collection<TopologyEditorMetadata> listTopologyEditorMetadata(List<QueryParam> queryParams) {
        return this.dao.find(TopologyEditorMetadata.NAME_SPACE, queryParams);
    }


    public TopologyEditorMetadata getTopologyEditorMetadata(Long topologyId) {
        return getTopologyEditorMetadata(topologyId, getCurrentVersionId(topologyId));
    }

    public TopologyEditorMetadata getTopologyEditorMetadata(Long topologyId, Long versionId) {
        TopologyEditorMetadata topologyEditorMetadata = new TopologyEditorMetadata();
        topologyEditorMetadata.setTopologyId(topologyId);
        topologyEditorMetadata.setVersionId(versionId);
        return this.dao.get(topologyEditorMetadata.getStorableKey());
    }

    public TopologyEditorMetadata addTopologyEditorMetadata(Long topologyId,
                                                            TopologyEditorMetadata topologyEditorMetadata) {
        return addTopologyEditorMetadata(topologyId, getCurrentVersionId(topologyId), topologyEditorMetadata);
    }

    public TopologyEditorMetadata addTopologyEditorMetadata(Long topologyId,
                                                            Long versionId,
                                                            TopologyEditorMetadata topologyEditorMetadata) {

        long timestamp = System.currentTimeMillis();
        topologyEditorMetadata.setTimestamp(timestamp);
        topologyEditorMetadata.setVersionId(versionId);
        this.dao.add(topologyEditorMetadata);
        updateVersionTimestamp(versionId, timestamp);
        return topologyEditorMetadata;
    }

    public TopologyEditorMetadata addOrUpdateTopologyEditorMetadata(Long topologyId, TopologyEditorMetadata topologyEditorMetadata) {
        Long currentTopologyVersionId = getCurrentVersionId(topologyId);
        topologyEditorMetadata.setTopologyId(topologyId);
        topologyEditorMetadata.setVersionId(currentTopologyVersionId);
        long timestamp = System.currentTimeMillis();
        topologyEditorMetadata.setTimestamp(timestamp);
        this.dao.addOrUpdate(topologyEditorMetadata);
        updateVersionTimestamp(currentTopologyVersionId, timestamp);
        return topologyEditorMetadata;
    }

    public TopologyEditorMetadata removeTopologyEditorMetadata(Long topologyId) {
        return removeTopologyEditorMetadata(topologyId, getCurrentVersionId(topologyId));
    }

    public TopologyEditorMetadata removeTopologyEditorMetadata(Long topologyId, Long versionId) {
        TopologyEditorMetadata topologyEditorMetadata = getTopologyEditorMetadata(topologyId, versionId);
        if (topologyEditorMetadata != null) {
            topologyEditorMetadata = dao.remove(topologyEditorMetadata.getStorableKey());
            topologyEditorMetadata.setTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return topologyEditorMetadata;
    }

    /**
     * returns the 'CURRENT' version of the source with given source Id
     */
    public TopologySource getTopologySource(Long topologyId, Long sourceId) {
        return getTopologySource(topologyId, sourceId, getCurrentVersionId(topologyId));
    }

    public TopologySource getTopologySource(Long topologyId, Long sourceId, Long versionId) {
        TopologySource topologySource = new TopologySource();
        topologySource.setId(sourceId);
        topologySource.setVersionId(versionId);
        TopologySource source = dao.get(new StorableKey(TOPOLOGY_SOURCE_NAMESPACE, topologySource.getPrimaryKey()));
        if (source == null || !source.getTopologyId().equals(topologyId)) {
            return null;
        }
        fillSourceStreams(source);
        return source;
    }

    /**
     * Generate id from the {@link TopologyComponent} namespace
     * so that its unique across source, sink and processors.
     * Similar to Table per concrete class hibernate strategy.
     */
    private Long getNextTopologyComponentId() {
        TopologyComponent component = new TopologyComponent();
        Long id = dao.nextId(TOPOLOGY_COMPONENT_NAMESPACE);
        component.setId(id);
        dao.add(component);
        dao.remove(component.getStorableKey());
        return component.getId();
    }

    /*
     * Handle this check at application layer since in-memory storage
     * does not contain unique key constraint.
     *
     * Other checks can be added later.
     */
    private void validateTopology(Topology topology) {
        StorageUtils.ensureUnique(topology, this::listTopologies,
                QueryParam.params(Topology.NAME, topology.getName()));
    }

    private void validateTopologySource(TopologySource topologySource) {
        StorageUtils.ensureUnique(topologySource, this::listTopologySources,
                QueryParam.params(TopologySource.TOPOLOGYID, topologySource.getTopologyId().toString(),
                        TopologySource.VERSIONID, topologySource.getVersionId().toString(),
                        TopologySource.NAME, topologySource.getName()));
    }

    private void validateTopologySink(TopologySink topologySink) {
        StorageUtils.ensureUnique(topologySink, this::listTopologySinks,
                QueryParam.params(TopologySink.TOPOLOGYID, topologySink.getTopologyId().toString(),
                        TopologySink.VERSIONID, topologySink.getVersionId().toString(),
                        TopologySink.NAME, topologySink.getName()));
    }

    private void validateTopologyProcessor(TopologyProcessor topologyProcessor) {
        StorageUtils.ensureUnique(topologyProcessor, this::listTopologyProcessors,
                QueryParam.params(TopologyProcessor.TOPOLOGYID, topologyProcessor.getTopologyId().toString(),
                        TopologyProcessor.VERSIONID, topologyProcessor.getVersionId().toString(),
                        TopologyProcessor.NAME, topologyProcessor.getName()));
    }

    public TopologySource addTopologySource(Long topologyId, TopologySource topologySource) {
        return addTopologySource(topologyId, getCurrentVersionId(topologyId), topologySource);
    }

    public TopologySource addTopologySource(Long topologyId,
                                            Long versionId,
                                            TopologySource topologySource) {
        if (topologySource.getId() == null) {
            topologySource.setId(getNextTopologyComponentId());
        }
        topologySource.setVersionId(versionId);
        topologySource.setTopologyId(topologyId);
        validateTopologySource(topologySource);
        List<TopologyStream> topologyStreams = addTopologyOutputComponent(topologySource);
        addSourceStreamMapping(topologySource, topologySource.getOutputStreamIds());
        topologySource.setOutputStreams(topologyStreams);
        topologySource.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        return topologySource;
    }

    private List<TopologyStream> addTopologyOutputComponent(TopologyOutputComponent outputComponent) {
        List<TopologyStream> topologyStreams;
        if (outputComponent.getOutputStreams() != null) {
            topologyStreams = addOutputStreams(outputComponent.getTopologyId(), outputComponent.getVersionId(),
                    outputComponent.getOutputStreams());
            outputComponent.setOutputStreamIds(new ArrayList<>(Collections2.transform(topologyStreams, new Function<TopologyStream, Long>() {
                @Override
                public Long apply(TopologyStream input) {
                    return input.getId();
                }
            })));
        } else if (outputComponent.getOutputStreamIds() != null) {
            topologyStreams = getOutputStreams(outputComponent.getTopologyId(), outputComponent.getVersionId(),
                    outputComponent.getOutputStreamIds());
        } else {
            topologyStreams = Collections.emptyList();
            outputComponent.setOutputStreamIds(Collections.<Long>emptyList());
        }
        dao.add(outputComponent);
        return topologyStreams;
    }

    private List<TopologyStream> getOutputStreams(Long topologyId, Long versionId, List<Long> outputStreamIds) {
        List<TopologyStream> topologyStreams = new ArrayList<>();
        for (Long outputStreamId : outputStreamIds) {
            TopologyStream topologyStream;
            if ((topologyStream = getStreamInfo(topologyId, outputStreamId, versionId)) == null) {
                throw new IllegalArgumentException("Output stream with id '" + outputStreamId + "' does not exist.");
            }
            topologyStreams.add(topologyStream);
        }
        return topologyStreams;
    }

    public TopologySource addOrUpdateTopologySource(Long topologyId, Long sourceId, TopologySource topologySource) {
        Long currentTopologyVersionId = getCurrentVersionId(topologyId);
        topologySource.setId(sourceId);
        topologySource.setVersionId(currentTopologyVersionId);
        topologySource.setTopologyId(topologyId);
        validateTopologySource(topologySource);
        dao.addOrUpdate(topologySource);
        List<Long> newList = Collections.emptyList();
        if (topologySource.getOutputStreamIds() != null) {
            newList = topologySource.getOutputStreamIds();
        } else if (topologySource.getOutputStreams() != null) {
            newList = updateOutputStreams(topologySource);
        }
        List<Long> existing = getOutputStreamIds(topologySource);
        Sets.SetView<Long> streamIdsToRemove = Sets.difference(ImmutableSet.copyOf(existing), ImmutableSet.copyOf(newList));
        Sets.SetView<Long> streamIdsToAdd = Sets.difference(ImmutableSet.copyOf(newList), ImmutableSet.copyOf(existing));
        removeSourceStreamMapping(topologySource, Lists.newArrayList(streamIdsToRemove));
        addSourceStreamMapping(topologySource, Lists.newArrayList(streamIdsToAdd));
        TopologySource updatedSource = getTopologySource(topologyId, sourceId, currentTopologyVersionId);
        updatedSource.setVersionTimestamp(updateVersionTimestamp(currentTopologyVersionId).getTimestamp());
        return updatedSource;
    }

    private List<Long> updateOutputStreams(TopologyOutputComponent outputComponent) {
        List<Long> newStreamIds = new ArrayList<>();
        for (TopologyStream topologyStream : outputComponent.getOutputStreams()) {
            if (topologyStream.getId() != null && getStreamInfo(outputComponent.getTopologyId(), topologyStream.getId()) != null) {
                addOrUpdateStreamInfo(outputComponent.getTopologyId(), topologyStream.getId(), topologyStream);
                newStreamIds.add(topologyStream.getId());
            } else {
                newStreamIds.add(addStreamInfo(outputComponent.getTopologyId(), topologyStream).getId());
            }
        }
        return newStreamIds;
    }

    public TopologySource removeTopologySource(Long topologyId, Long sourceId, boolean removeEdges) {
        return removeTopologySource(topologyId, sourceId, getCurrentVersionId(topologyId), removeEdges);
    }

    public TopologySource removeTopologySource(Long topologyId, Long sourceId, Long versionId, boolean removeEdges) {
        TopologySource topologySource = getTopologySource(topologyId, sourceId, versionId);
        if (topologySource != null) {
            if (removeEdges) {
                removeAllEdges(topologySource);
            }
            removeSourceStreamMapping(topologySource);
            topologySource = dao.<TopologySource>remove(new StorableKey(TOPOLOGY_SOURCE_NAMESPACE, topologySource.getPrimaryKey()));
            topologySource.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return topologySource;
    }

    // removes any incoming or outgoing edges from the component
    private void removeAllEdges(TopologyComponent c) {
        removeTopologyEdge(buildEdgesFromQueryParam(c.getTopologyId(), c.getVersionId(), c.getId()));
        removeTopologyEdge(buildEdgesToQueryParam(c.getTopologyId(), c.getVersionId(), c.getId()));
    }

    private void removeTopologyEdge(List<QueryParam> queryParams) {
        try {
            listTopologyEdges(queryParams).forEach(edge -> {
                LOG.debug("Removing edge {}", edge);
                removeTopologyEdge(edge.getTopologyId(), edge.getId(), edge.getVersionId());
            });
        } catch (Exception ex) {
            LOG.error("Got exception while removing edge", ex);
            throw new RuntimeException(ex);
        }
    }

    public Collection<TopologySource> listTopologySources() {
        return fillSourceStreams(dao.<TopologySource>list(TOPOLOGY_SOURCE_NAMESPACE));
    }

    public Collection<TopologySource> listTopologySources(List<QueryParam> params) {
        return fillSourceStreams(dao.<TopologySource>find(TOPOLOGY_SOURCE_NAMESPACE, params));
    }

    private List<TopologyStream> addOutputStreams(Long topologyId, Long versionId, List<TopologyStream> streams) {
        List<TopologyStream> topologyStreams = new ArrayList<>();
        for (TopologyStream outputStream : streams) {
            topologyStreams.add(addStreamInfo(topologyId, versionId, outputStream));
        }
        return topologyStreams;
    }

    private void addSourceStreamMapping(TopologySource topologySource, List<Long> streamIds) {
        for (Long outputStreamId : streamIds) {
            dao.<TopologySourceStreamMapping>add(new TopologySourceStreamMapping(topologySource.getId(),
                    topologySource.getVersionId(), outputStreamId));
        }
    }

    private void removeSourceStreamMapping(TopologySource topologySource) {
        if (topologySource != null) {
            removeSourceStreamMapping(topologySource, topologySource.getOutputStreamIds());
        }
    }

    private void removeSourceStreamMapping(TopologySource topologySource, List<Long> streamIds) {
        if (topologySource != null) {
            for (Long outputStreamId : streamIds) {
                TopologySourceStreamMapping mapping = new TopologySourceStreamMapping(
                        topologySource.getId(),
                        topologySource.getVersionId(),
                        outputStreamId);
                dao.<TopologySourceStreamMapping>remove(mapping.getStorableKey());
            }
        }
    }

    private List<Long> getOutputStreamIds(TopologySource topologySource) {
        List<Long> streamIds = new ArrayList<>();
        if (topologySource != null) {
            QueryParam qp1 = new QueryParam(TopologySourceStreamMapping.FIELD_SOURCE_ID,
                    String.valueOf(topologySource.getId()));
            QueryParam qp2 = new QueryParam(TopologySourceStreamMapping.FIELD_VERSION_ID,
                    String.valueOf(topologySource.getVersionId()));
            for (TopologySourceStreamMapping mapping : listTopologySourceStreamMapping(ImmutableList.of(qp1, qp2))) {
                streamIds.add(mapping.getStreamId());
            }
        }
        return streamIds;
    }

    private Collection<TopologySourceStreamMapping> listTopologySourceStreamMapping(List<QueryParam> params) {
        try {
            return dao.find(TOPOLOGY_SOURCE_STREAM_MAPPING_NAMESPACE, params);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void fillProcessorStreams(TopologyProcessor processor) {
        if (processor != null) {
            fillProcessorStreams(Collections.singletonList(processor));
        }
    }

    private Collection<TopologyProcessor> fillProcessorStreams(Collection<TopologyProcessor> processors) {
        if (processors != null) {
            for (TopologyProcessor processor : processors) {
                List<TopologyStream> topologyStreams = getOutputStreams(processor);
                processor.setOutputStreams(topologyStreams);
                processor.setOutputStreamIds(new ArrayList<>(Collections2.transform(topologyStreams, new Function<TopologyStream, Long>() {
                    @Nullable
                    @Override
                    public Long apply(@Nullable TopologyStream input) {
                        return input.getId();
                    }
                })));
            }
        }
        return processors;
    }

    private List<TopologyStream> getOutputStreams(TopologyProcessor topologyProcessor) {
        List<TopologyStream> streams = new ArrayList<>();
        if (topologyProcessor != null) {
            QueryParam qp1 = new QueryParam(TopologyProcessorStreamMapping.FIELD_PROCESSOR_ID,
                    String.valueOf(topologyProcessor.getId()));
            QueryParam qp2 = new QueryParam(TopologyProcessorStreamMapping.FIELD_VERSION_ID,
                    String.valueOf(topologyProcessor.getVersionId()));
            for (TopologyProcessorStreamMapping mapping : listTopologyProcessorStreamMapping(ImmutableList.of(qp1, qp2))) {
                TopologyStream topologyStream = getStreamInfo(topologyProcessor.getTopologyId(), mapping.getStreamId(), topologyProcessor.getVersionId());
                if (topologyStream != null) {
                    streams.add(topologyStream);
                }
            }
        }
        return streams;
    }

    private void fillSourceStreams(TopologySource source) {
        if (source != null) {
            fillSourceStreams(Collections.singletonList(source));
        }
    }

    private Collection<TopologySource> fillSourceStreams(Collection<TopologySource> sources) {
        if (sources != null) {
            for (TopologySource source : sources) {
                List<TopologyStream> topologyStreams = getOutputStreams(source);
                source.setOutputStreams(topologyStreams);
                source.setOutputStreamIds(new ArrayList<>(Collections2.transform(topologyStreams, new Function<TopologyStream, Long>() {
                    @Nullable
                    @Override
                    public Long apply(@Nullable TopologyStream input) {
                        return input.getId();
                    }
                })));
            }
        }
        return sources;
    }

    private List<TopologyStream> getOutputStreams(TopologySource topologySource) {
        List<TopologyStream> streams = new ArrayList<>();
        if (topologySource != null) {
            QueryParam qp1 = new QueryParam(TopologySourceStreamMapping.FIELD_SOURCE_ID,
                    String.valueOf(topologySource.getId()));
            QueryParam qp2 = new QueryParam(TopologySourceStreamMapping.FIELD_VERSION_ID,
                    String.valueOf(topologySource.getVersionId()));
            for (TopologySourceStreamMapping mapping : listTopologySourceStreamMapping(ImmutableList.of(qp1, qp2))) {
                TopologyStream topologyStream = getStreamInfo(topologySource.getTopologyId(), mapping.getStreamId(), topologySource.getVersionId());
                if (topologyStream != null) {
                    streams.add(topologyStream);
                }
            }
        }
        return streams;
    }

    public TopologySink getTopologySink(Long topologyId, Long sinkId) {
        return getTopologySink(topologyId, sinkId, getCurrentVersionId(topologyId));
    }

    public TopologySink getTopologySink(Long topologyId, Long sinkId, Long versionId) {
        TopologySink topologySink = new TopologySink();
        topologySink.setId(sinkId);
        topologySink.setVersionId(versionId);
        TopologySink sink = dao.get(new StorableKey(TOPOLOGY_SINK_NAMESPACE, topologySink.getPrimaryKey()));
        if (sink == null || !sink.getTopologyId().equals(topologyId)) {
            return null;
        }
        sink.setVersionTimestamp(getVersionTimestamp(versionId));
        return sink;
    }

    public TopologySink addTopologySink(Long topologyId, TopologySink topologySink) {
        return addTopologySink(topologyId, getCurrentVersionId(topologyId), topologySink);
    }

    public TopologySink addTopologySink(Long topologyId,
                                        Long versionId,
                                        TopologySink topologySink) {
        if (topologySink.getId() == null) {
            topologySink.setId(getNextTopologyComponentId());
        }
        topologySink.setVersionId(versionId);
        topologySink.setTopologyId(topologyId);
        validateTopologySink(topologySink);
        dao.add(topologySink);
        topologySink.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        return topologySink;
    }

    public TopologySink addOrUpdateTopologySink(Long topologyId, Long id, TopologySink topologySink) {
        Long currentTopologyVersionId = getCurrentVersionId(topologyId);
        topologySink.setId(id);
        topologySink.setVersionId(currentTopologyVersionId);
        topologySink.setTopologyId(topologyId);
        validateTopologySink(topologySink);
        dao.addOrUpdate(topologySink);
        topologySink.setVersionTimestamp(updateVersionTimestamp(currentTopologyVersionId).getTimestamp());
        return topologySink;
    }

    public TopologySink removeTopologySink(Long id) {
        TopologySink topologySink = new TopologySink();
        topologySink.setId(id);
        return dao.remove(new StorableKey(TOPOLOGY_SINK_NAMESPACE, topologySink.getPrimaryKey()));
    }

    public TopologySink removeTopologySink(Long topologyId, Long sinkId, boolean removeEdges) {
        return removeTopologySink(topologyId, sinkId, getCurrentVersionId(topologyId), removeEdges);
    }

    public TopologySink removeTopologySink(Long topologyId, Long sinkId, Long versionId, boolean removeEdges) {
        TopologySink topologySink = getTopologySink(topologyId, sinkId, versionId);
        if (topologySink != null) {
            if (removeEdges) {
                removeAllEdges(topologySink);
            }
            topologySink = dao.<TopologySink>remove(new StorableKey(TOPOLOGY_SINK_NAMESPACE, topologySink.getPrimaryKey()));
            topologySink.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return topologySink;
    }

    public Collection<TopologySink> listTopologySinks() {
        return dao.list(TOPOLOGY_SINK_NAMESPACE);
    }

    public Collection<TopologySink> listTopologySinks(List<QueryParam> params) {
        return dao.find(TOPOLOGY_SINK_NAMESPACE, params);
    }

    public TopologyProcessor getTopologyProcessor(Long topologyId, Long processorId) {
        return getTopologyProcessor(topologyId, processorId, getCurrentVersionId(topologyId));
    }

    public TopologyProcessor getTopologyProcessor(Long topologyId, Long processorId, Long versionId) {
        TopologyProcessor topologyProcessor = new TopologyProcessor();
        topologyProcessor.setId(processorId);
        topologyProcessor.setVersionId(versionId);
        TopologyProcessor processor = dao.get(new StorableKey(TOPOLOGY_PROCESSOR_NAMESPACE, topologyProcessor.getPrimaryKey()));
        if (processor == null || !processor.getTopologyId().equals(topologyId)) {
            return null;
        }
        fillProcessorStreams(processor);
        processor.setVersionTimestamp(getVersionTimestamp(versionId));
        return processor;
    }

    public TopologyProcessor addTopologyProcessor(Long topologyId, TopologyProcessor topologyProcessor) {
        return addTopologyProcessor(topologyId, getCurrentVersionId(topologyId), topologyProcessor);
    }

    public TopologyProcessor addTopologyProcessor(Long topologyId,
                                                  Long versionId,
                                                  TopologyProcessor topologyProcessor) {
        if (topologyProcessor.getId() == null) {
            topologyProcessor.setId(getNextTopologyComponentId());
        }
        topologyProcessor.setVersionId(versionId);
        topologyProcessor.setTopologyId(topologyId);
        validateTopologyProcessor(topologyProcessor);
        List<TopologyStream> topologyStreams = addTopologyOutputComponent(topologyProcessor);
        addProcessorStreamMapping(topologyProcessor, topologyProcessor.getOutputStreamIds());
        topologyProcessor.setOutputStreams(topologyStreams);
        topologyProcessor.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        return topologyProcessor;
    }

    public TopologyProcessor addOrUpdateTopologyProcessor(Long topologyId, Long id, TopologyProcessor topologyProcessor) {
        Long currentTopologyVersionId = getCurrentVersionId(topologyId);
        topologyProcessor.setId(id);
        topologyProcessor.setVersionId(currentTopologyVersionId);
        topologyProcessor.setTopologyId(topologyId);
        validateTopologyProcessor(topologyProcessor);
        dao.addOrUpdate(topologyProcessor);
        List<Long> newList = Collections.emptyList();
        if (topologyProcessor.getOutputStreamIds() != null) {
            newList = topologyProcessor.getOutputStreamIds();
        } else if (topologyProcessor.getOutputStreams() != null) {
            newList = updateOutputStreams(topologyProcessor);
        }
        List<Long> existing = getOutputStreamIds(topologyProcessor);
        Sets.SetView<Long> streamIdsToRemove = Sets.difference(ImmutableSet.copyOf(existing), ImmutableSet.copyOf(newList));
        Sets.SetView<Long> streamIdsToAdd = Sets.difference(ImmutableSet.copyOf(newList), ImmutableSet.copyOf(existing));
        removeProcessorStreamMapping(topologyProcessor, Lists.newArrayList(streamIdsToRemove));
        addProcessorStreamMapping(topologyProcessor, Lists.newArrayList(streamIdsToAdd));
        TopologyProcessor updatedProcessor = getTopologyProcessor(topologyId, id, currentTopologyVersionId);
        updatedProcessor.setVersionTimestamp(updateVersionTimestamp(currentTopologyVersionId).getTimestamp());
        return topologyProcessor;
    }

    public TopologyProcessor removeTopologyProcessor(Long topologyId, Long processorId, boolean removeEdges) {
        return removeTopologyProcessor(topologyId, processorId, getCurrentVersionId(topologyId), removeEdges);
    }

    public TopologyProcessor removeTopologyProcessor(Long topologyId, Long processorId, Long versionId, boolean removeEdges) {
        TopologyProcessor topologyProcessor = getTopologyProcessor(topologyId, processorId, versionId);
        if (topologyProcessor != null) {
            if (removeEdges) {
                removeAllEdges(topologyProcessor);
            }
            removeProcessorStreamMapping(topologyProcessor);
            topologyProcessor = dao.<TopologyProcessor>remove(new StorableKey(TOPOLOGY_PROCESSOR_NAMESPACE, topologyProcessor.getPrimaryKey()));
            topologyProcessor.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return topologyProcessor;
    }

    public Collection<TopologyProcessor> listTopologyProcessors() {
        return fillProcessorStreams(dao.<TopologyProcessor>list(TOPOLOGY_PROCESSOR_NAMESPACE));
    }

    public Collection<TopologyProcessor> listTopologyProcessors(List<QueryParam> params) {
        return fillProcessorStreams(dao.<TopologyProcessor>find(TOPOLOGY_PROCESSOR_NAMESPACE, params));
    }

    private void createProcessorStreamMapping(TopologyProcessor topologyProcessor, List<TopologyStream> streams) {
        for (TopologyStream outputStream : streams) {
            TopologyStream addedStream = addStreamInfo(topologyProcessor.getTopologyId(), outputStream);
            dao.<TopologyProcessorStreamMapping>add(new TopologyProcessorStreamMapping(topologyProcessor.getId(),
                    topologyProcessor.getVersionId(),
                    addedStream.getId()));
        }
    }

    private void addProcessorStreamMapping(TopologyProcessor topologyProcessor, List<Long> streamIds) {
        for (Long outputStreamId : streamIds) {
            dao.<TopologyProcessorStreamMapping>add(new TopologyProcessorStreamMapping(topologyProcessor.getId(),
                    topologyProcessor.getVersionId(),
                    outputStreamId));
        }
    }

    private void removeProcessorStreamMapping(TopologyProcessor topologyProcessor) {
        if (topologyProcessor != null) {
            removeProcessorStreamMapping(topologyProcessor, topologyProcessor.getOutputStreamIds());
        }
    }

    private void removeProcessorStreamMapping(TopologyProcessor topologyProcessor, List<Long> streamIds) {
        if (topologyProcessor != null) {
            for (Long outputStreamId : streamIds) {
                TopologyProcessorStreamMapping mapping = new TopologyProcessorStreamMapping(topologyProcessor.getId(),
                        topologyProcessor.getVersionId(),
                        outputStreamId);
                dao.<TopologyProcessorStreamMapping>remove(mapping.getStorableKey());
            }
        }
    }

    private List<Long> getOutputStreamIds(TopologyProcessor topologyProcessor) {
        List<Long> streamIds = new ArrayList<>();
        if (topologyProcessor != null) {
            QueryParam qp1 = new QueryParam(TopologyProcessorStreamMapping.FIELD_PROCESSOR_ID,
                    String.valueOf(topologyProcessor.getId()));
            QueryParam qp2 = new QueryParam(TopologyProcessorStreamMapping.FIELD_VERSION_ID,
                    String.valueOf(topologyProcessor.getVersionId()));
            for (TopologyProcessorStreamMapping mapping : listTopologyProcessorStreamMapping(ImmutableList.of(qp1, qp2))) {
                streamIds.add(mapping.getStreamId());
            }
        }
        return streamIds;
    }

    private Collection<TopologyProcessorStreamMapping> listTopologyProcessorStreamMapping(List<QueryParam> params) {
        try {
            return dao.find(TOPOLOGY_PROCESSOR_STREAM_MAPPING_NAMESPACE, params);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public TopologyEdge getTopologyEdge(Long topologyId, Long edgeId) {
        return getTopologyEdge(topologyId, edgeId, getCurrentVersionId(topologyId));
    }

    public TopologyEdge getTopologyEdge(Long topologyId, Long edgeId, Long versionId) {
        TopologyEdge topologyEdge = new TopologyEdge();
        topologyEdge.setId(edgeId);
        topologyEdge.setVersionId(versionId);
        TopologyEdge edge = dao.get(new StorableKey(TOPOLOGY_EDGE_NAMESPACE, topologyEdge.getPrimaryKey()));
        if (edge == null || !edge.getTopologyId().equals(topologyId)) {
            return null;
        }
        edge.setVersionTimestamp(getVersionTimestamp(versionId));
        return edge;
    }

    public TopologyEdge addTopologyEdge(Long topologyId, TopologyEdge topologyEdge) {
        return addTopologyEdge(topologyId, getCurrentVersionId(topologyId), topologyEdge);
    }

    public TopologyEdge addTopologyEdge(Long topologyId,
                                        Long versionId,
                                        TopologyEdge topologyEdge) {
        if (topologyEdge.getId() == null) {
            topologyEdge.setId(dao.nextId(TOPOLOGY_EDGE_NAMESPACE));
        }
        topologyEdge.setVersionId(versionId);
        topologyEdge.setTopologyId(topologyId);
        validateEdge(topologyEdge);
        checkDuplicateEdge(topologyEdge);
        dao.add(topologyEdge);
        topologyEdge.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        return topologyEdge;
    }

    // validate from, to and stream ids of the edge
    private void validateEdge(TopologyEdge edge) {
        TopologySource source = getTopologySource(edge.getTopologyId(), edge.getFromId(), edge.getVersionId());
        TopologyProcessor processor = getTopologyProcessor(edge.getTopologyId(), edge.getFromId(), edge.getVersionId());
        if ((source == null || !source.getTopologyId().equals(edge.getTopologyId()))
                && (processor == null || !processor.getTopologyId().equals(edge.getTopologyId()))) {
            throw new IllegalArgumentException("Invalid source for edge " + edge);
        }
        TopologyOutputComponent outputComponent = source != null ? source : processor;
        processor = getTopologyProcessor(edge.getTopologyId(), edge.getToId(), edge.getVersionId());
        TopologySink sink = getTopologySink(edge.getTopologyId(), edge.getToId(), edge.getVersionId());
        if ((processor == null || !processor.getTopologyId().equals(edge.getTopologyId()))
                && (sink == null || !sink.getTopologyId().equals(edge.getTopologyId()))) {
            throw new IllegalArgumentException("Invalid destination for edge " + edge);
        }

        Set<Long> outputStreamIds = new HashSet<>();
        if (outputComponent.getOutputStreamIds() != null) {
            outputStreamIds.addAll(outputComponent.getOutputStreamIds());
        } else if (outputComponent.getOutputStreams() != null) {
            outputStreamIds.addAll(Collections2.transform(outputComponent.getOutputStreams(), new Function<TopologyStream, Long>() {
                @Override
                public Long apply(TopologyStream input) {
                    return input.getId();
                }
            }));
        }

        Collection<Long> edgeStreamIds = Collections2.transform(edge.getStreamGroupings(),
                                                                new Function<StreamGrouping, Long>() {
                                                                    public Long apply(StreamGrouping streamGrouping) {
                                                                        return streamGrouping.getStreamId();
                                                                    }
                                                                });
        if (!outputStreamIds.containsAll(edgeStreamIds)) {
            throw new IllegalArgumentException("Edge stream Ids " + edgeStreamIds +
                                                       " must be a subset of outputStreamIds " + outputStreamIds);
        }
        // check the fields specified in the fields grouping is a subset of the stream fields
        for (StreamGrouping streamGrouping : edge.getStreamGroupings()) {
            List<String> fields;
            if ((fields = streamGrouping.getFields()) != null) {
                Set<String> schemaFieldPatterns = getFieldPatterns(
                        getStreamInfo(edge.getTopologyId(), streamGrouping.getStreamId(), edge.getVersionId())
                                .getFields());
                fields.forEach(field -> {
                    schemaFieldPatterns.stream().filter(pat -> field.matches(pat)).findAny()
                            .orElseThrow(() -> new IllegalArgumentException("Fields in the grouping " + fields +
                                    " must be a subset the stream fields " + schemaFieldPatterns));
                });
            }
        }
    }

    private Set<String> getFieldPatterns(List<Schema.Field> fields) {
        if (fields == null || fields.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> names = new HashSet<>();
        fields.forEach(field -> {
            if (field.getType() == Schema.Type.NESTED) {
                getFieldPatterns(((Schema.NestedField) field).getFields())
                        .forEach(childFieldName -> names.add(field.getName() + "." + childFieldName));
            } else if (field.getType() == Schema.Type.ARRAY) {
                String pattern;
                if (field.getName() == null || field.getName().isEmpty()) {
                    pattern = "\\[\\d+\\]";
                } else {
                    pattern = field.getName() + "\\[\\d+\\]";
                }
                ((Schema.ArrayField) field).getMembers()
                        .forEach(member -> {
                            getFieldPatterns(Collections.singletonList(member))
                                    .forEach(pat -> {
                                        if (member.getType() == Schema.Type.ARRAY) {
                                            names.add(pattern + pat);
                                        } else if (member.getType() == Schema.Type.NESTED) {
                                            names.add(pattern + "." + pat);
                                        }
                                    });
                        });
            } else {
                names.add(field.getName());
            }
        });
        LOG.debug("Field names {}", names);
        return names;
    }

    // check if edge already exists for given topology between same source and dest
    private void checkDuplicateEdge(TopologyEdge edge) {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam(TopologyEdge.TOPOLOGYID, edge.getTopologyId().toString()));
        queryParams.add(new QueryParam(TopologyEdge.VERSIONID, edge.getVersionId().toString()));
        queryParams.add(new QueryParam(TopologyEdge.FROMID, edge.getFromId().toString()));
        queryParams.add(new QueryParam(TopologyEdge.TOID, edge.getToId().toString()));

        try {
            Collection<TopologyEdge> existingEdges = listTopologyEdges(queryParams);
            if (existingEdges != null && !existingEdges.isEmpty()) {
                throw new IllegalArgumentException("Edge already exists between source and destination, use update api");
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public TopologyEdge addOrUpdateTopologyEdge(Long topologyId, Long id, TopologyEdge topologyEdge) {
        Long currentTopologyVersionId = getCurrentVersionId(topologyId);
        topologyEdge.setId(id);
        topologyEdge.setVersionId(currentTopologyVersionId);
        topologyEdge.setTopologyId(topologyId);
        validateEdge(topologyEdge);
        dao.addOrUpdate(topologyEdge);
        topologyEdge.setVersionTimestamp(updateVersionTimestamp(currentTopologyVersionId).getTimestamp());
        return topologyEdge;
    }

    public TopologyEdge removeTopologyEdge(Long topologyId, Long edgeId) {
        return removeTopologyEdge(topologyId, edgeId, getCurrentVersionId(topologyId));
    }

    public TopologyEdge removeTopologyEdge(Long topologyId, Long edgeId, Long versionId) {
        TopologyEdge topologyEdge = getTopologyEdge(topologyId, edgeId, versionId);
        if (topologyEdge != null) {
            topologyEdge = dao.remove(new StorableKey(TOPOLOGY_EDGE_NAMESPACE, topologyEdge.getPrimaryKey()));
            topologyEdge.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return topologyEdge;
    }

    public Collection<TopologyEdge> listTopologyEdges() {
        return dao.list(TOPOLOGY_EDGE_NAMESPACE);
    }

    public Collection<TopologyEdge> listTopologyEdges(List<QueryParam> params) throws Exception {
        return dao.find(TOPOLOGY_EDGE_NAMESPACE, params);
    }

    public TopologyStream getStreamInfo(Long topologyId, Long streamId) {
        return getStreamInfo(topologyId, streamId, getCurrentVersionId(topologyId));
    }

    public TopologyStream getStreamInfo(Long topologyId, Long streamId, Long versionId) {
        TopologyStream topologyStream = new TopologyStream();
        topologyStream.setId(streamId);
        topologyStream.setVersionId(versionId);
        TopologyStream result = dao.get(new StorableKey(STREAMINFO_NAMESPACE, topologyStream.getPrimaryKey()));
        if (result == null || !result.getTopologyId().equals(topologyId)) {
            return null;
        }
        result.setVersionTimestamp(getVersionTimestamp(versionId));
        return result;
    }

    public TopologyStream getStreamInfoByName(Long topologyId, String streamId) {
        return getStreamInfoByName(topologyId, streamId, getCurrentVersionId(topologyId));
    }
    public TopologyStream getStreamInfoByName(Long topologyId,
                                              String streamId,
                                              Long versionId) {
        List<QueryParam> queryParams = WSUtils.buildTopologyIdAndVersionIdAwareQueryParams(topologyId, versionId, null);
        try {
            for (TopologyStream topologyStream : listStreamInfos(queryParams)) {
                if (topologyStream.getStreamId().equals(streamId)) {
                    return topologyStream;
                }
            }
        } catch (Exception ex) {
            LOG.error("Got exception ", ex);
            throw new RuntimeException(ex);
        }
      return null;
    }

    private void validateStreamInfo(TopologyStream topologyStream) {
        if (topologyStream.getFields().isEmpty()) {
            throw new IllegalArgumentException("Stream with empty fields: " + topologyStream);
        }
        StorageUtils.ensureUnique(topologyStream, this::listStreamInfos,
                QueryParam.params(TopologyStream.TOPOLOGYID, topologyStream.getTopologyId().toString(),
                        TopologyStream.VERSIONID, topologyStream.getVersionId().toString(),
                        TopologyStream.STREAMID, topologyStream.getStreamId()));
    }

    public TopologyStream addStreamInfo(Long topologyId, TopologyStream topologyStream) {
        return addStreamInfo(topologyId, getCurrentVersionId(topologyId), topologyStream);
    }

    public TopologyStream addStreamInfo(Long topologyId,
                                        Long versionId,
                                        TopologyStream topologyStream) {
        if (topologyStream.getId() == null) {
            topologyStream.setId(dao.nextId(STREAMINFO_NAMESPACE));
        }
        long timestamp = System.currentTimeMillis();
        topologyStream.setVersionTimestamp(timestamp);
        topologyStream.setVersionId(versionId);
        topologyStream.setTopologyId(topologyId);
        validateStreamInfo(topologyStream);
        dao.add(topologyStream);
        updateVersionTimestamp(versionId, timestamp);
        return topologyStream;
    }

    public TopologyStream addOrUpdateStreamInfo(Long topologyId, Long id, TopologyStream stream) {
        stream.setId(id);
        Long currentVersionId = getCurrentVersionId(topologyId);
        stream.setVersionId(currentVersionId);
        stream.setTopologyId(topologyId);
        long timestamp = System.currentTimeMillis();
        stream.setVersionTimestamp(timestamp);
        validateStreamInfo(stream);
        dao.addOrUpdate(stream);
        updateVersionTimestamp(currentVersionId, timestamp);
        return stream;
    }

    public TopologyStream removeStreamInfo(Long topologyId, Long streamId) {
        return removeStreamInfo(topologyId, streamId, getCurrentVersionId(topologyId));
    }

    public TopologyStream removeStreamInfo(Long topologyId, Long streamId, Long versionId) {
        TopologyStream topologyStream = getStreamInfo(topologyId, streamId, versionId);
        if (topologyStream != null) {
            topologyStream = dao.remove(new StorableKey(STREAMINFO_NAMESPACE, topologyStream.getPrimaryKey()));
            topologyStream.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return topologyStream;
    }

    public Collection<TopologyStream> listStreamInfos() {
        return dao.list(STREAMINFO_NAMESPACE);
    }

    public Collection<TopologyStream> listStreamInfos(List<QueryParam> params) {
        return dao.find(STREAMINFO_NAMESPACE, params);
    }

    public Collection<TopologyRule> listRules() {
        return dao.list(TOPOLOGY_RULEINFO_NAMESPACE);
    }

    public Collection<TopologyRule> listRules(List<QueryParam> params) throws Exception {
        return dao.find(TOPOLOGY_RULEINFO_NAMESPACE, params);
    }

    public TopologyRule addRule(Long topologyId, TopologyRule topologyRule) throws Exception {
        return addRule(topologyId, getCurrentVersionId(topologyId), topologyRule);
    }

    public TopologyRule addRule(Long topologyId,
                                Long versionId,
                                TopologyRule topologyRule) throws Exception {
        if (topologyRule.getId() == null) {
            topologyRule.setId(dao.nextId(TOPOLOGY_RULEINFO_NAMESPACE));
        }
        topologyRule.setVersionId(versionId);
        topologyRule.setTopologyId(topologyId);
        String parsedRuleStr = parseAndSerialize(topologyRule);
        LOG.debug("ParsedRuleStr {}", parsedRuleStr);
        topologyRule.setParsedRuleStr(parsedRuleStr);
        dao.add(topologyRule);
        topologyRule.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        return topologyRule;
    }

    public TopologyRule getRule(Long topologyId, Long ruleId) throws Exception {
        return getRule(topologyId, ruleId, getCurrentVersionId(topologyId));
    }

    public TopologyRule getRule(Long topologyId, Long ruleId, Long versionId) throws Exception {
        TopologyRule topologyTopologyRule = new TopologyRule();
        topologyTopologyRule.setId(ruleId);
        topologyTopologyRule.setVersionId(versionId);
        TopologyRule ruleInfo = dao.get(new StorableKey(TOPOLOGY_RULEINFO_NAMESPACE, topologyTopologyRule.getPrimaryKey()));
        if (ruleInfo == null || !ruleInfo.getTopologyId().equals(topologyId)) {
            return null;
        }
        ruleInfo.setVersionTimestamp(getVersionTimestamp(versionId));
        return ruleInfo;
    }


    public TopologyRule addOrUpdateRule(Long topologyId, Long ruleId, TopologyRule topologyRule) throws Exception {
        Long currentTopologyVersionId = getCurrentVersionId(topologyId);
        topologyRule.setId(ruleId);
        topologyRule.setVersionId(currentTopologyVersionId);
        topologyRule.setTopologyId(topologyId);
        String parsedRuleStr = parseAndSerialize(topologyRule);
        LOG.debug("ParsedRuleStr {}", parsedRuleStr);
        topologyRule.setParsedRuleStr(parsedRuleStr);
        dao.addOrUpdate(topologyRule);
        topologyRule.setVersionTimestamp(updateVersionTimestamp(currentTopologyVersionId).getTimestamp());
        return topologyRule;
    }

    public TopologyRule removeRule(Long topologyId, Long ruleId) throws Exception {
        return removeRule(topologyId, ruleId, getCurrentVersionId(topologyId));
    }

    public TopologyRule removeRule(Long topologyId, Long ruleId, Long versionId) throws Exception {
        TopologyRule topologyRule = getRule(topologyId, ruleId, versionId);
        if (topologyRule != null) {
            topologyRule = dao.remove(new StorableKey(TOPOLOGY_RULEINFO_NAMESPACE, topologyRule.getPrimaryKey()));
            topologyRule.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return topologyRule;
    }

    public Collection<TopologyWindow> listWindows() {
        return dao.list(TOPOLOGY_WINDOWINFO_NAMESPACE);
    }

    public Collection<TopologyWindow> listWindows(List<QueryParam> params) throws Exception {
        return dao.find(TOPOLOGY_WINDOWINFO_NAMESPACE, params);
    }

    public Collection<TopologyBranchRule> listBranchRules() {
        return dao.list(TOPOLOGY_BRANCHRULEINFO_NAMESPACE);
    }

    public Collection<TopologyBranchRule> listBranchRules(List<QueryParam> params) throws Exception {
        return dao.find(TOPOLOGY_BRANCHRULEINFO_NAMESPACE, params);
    }

    public TopologyBranchRule addBranchRule(Long topologyId, TopologyBranchRule topologyBranchRule) throws Exception {
        return addBranchRule(topologyId, getCurrentVersionId(topologyId), topologyBranchRule);
    }
    public TopologyBranchRule addBranchRule(Long topologyId,
                                            Long versionId,
                                            TopologyBranchRule topologyBranchRule) throws Exception {
        if (topologyBranchRule.getId() == null) {
            topologyBranchRule.setId(dao.nextId(TOPOLOGY_BRANCHRULEINFO_NAMESPACE));
        }
        topologyBranchRule.setTopologyId(topologyId);
        topologyBranchRule.setVersionId(versionId);
        String parsedRuleStr = parseAndSerialize(topologyBranchRule);
        LOG.debug("ParsedRuleStr {}", parsedRuleStr);
        topologyBranchRule.setParsedRuleStr(parsedRuleStr);
        dao.add(topologyBranchRule);
        topologyBranchRule.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        return topologyBranchRule;
    }

    public TopologyBranchRule getBranchRule(Long topologyId, Long ruleId) throws Exception {
        return getBranchRule(topologyId, ruleId, getCurrentVersionId(topologyId));
    }

    public TopologyBranchRule getBranchRule(Long topologyId, Long ruleId, Long versionId) throws Exception {
        TopologyBranchRule topologyBranchRule = new TopologyBranchRule();
        topologyBranchRule.setId(ruleId);
        topologyBranchRule.setVersionId(versionId);
        topologyBranchRule = dao.get(new StorableKey(TOPOLOGY_BRANCHRULEINFO_NAMESPACE, topologyBranchRule.getPrimaryKey()));
        if (topologyBranchRule == null || !topologyBranchRule.getTopologyId().equals(topologyId)) {
            return null;
        }
        topologyBranchRule.setVersionTimestamp(getVersionTimestamp(versionId));
        return topologyBranchRule;
    }

    public TopologyBranchRule addOrUpdateBranchRule(Long topologyId, Long ruleId, TopologyBranchRule topologyBranchRule) throws Exception {
        Long currentTopologyVersionId = getCurrentVersionId(topologyId);
        topologyBranchRule.setId(ruleId);
        topologyBranchRule.setVersionId(currentTopologyVersionId);
        topologyBranchRule.setTopologyId(topologyId);
        String parsedRuleStr = parseAndSerialize(topologyBranchRule);
        LOG.debug("ParsedRuleStr {}", parsedRuleStr);
        topologyBranchRule.setParsedRuleStr(parsedRuleStr);
        dao.addOrUpdate(topologyBranchRule);
        topologyBranchRule.setVersionTimestamp(updateVersionTimestamp(currentTopologyVersionId).getTimestamp());
        return topologyBranchRule;
    }

    public TopologyBranchRule removeBranchRule(Long topologyId, Long id) throws Exception {
        return removeBranchRule(topologyId, id, getCurrentVersionId(topologyId));
    }

    public TopologyBranchRule removeBranchRule(Long topologyId, Long id, Long versionId) throws Exception {
        TopologyBranchRule topologyBranchRule = getBranchRule(topologyId, id, versionId);
        if (topologyBranchRule != null) {
            topologyBranchRule = dao.remove(new StorableKey(TOPOLOGY_BRANCHRULEINFO_NAMESPACE, topologyBranchRule.getPrimaryKey()));
            topologyBranchRule.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return topologyBranchRule;
    }

    public TopologyWindow addWindow(Long topologyId, TopologyWindow topologyWindow) throws Exception {
        return addWindow(topologyId, getCurrentVersionId(topologyId), topologyWindow);
    }

    public TopologyWindow addWindow(Long topologyId,
                                    Long versionId,
                                    TopologyWindow topologyWindow) throws Exception {
        if (topologyWindow.getId() == null) {
            topologyWindow.setId(dao.nextId(TOPOLOGY_WINDOWINFO_NAMESPACE));
        }
        topologyWindow.setTopologyId(topologyId);
        topologyWindow.setVersionId(versionId);
        String parsedRuleStr = parseAndSerialize(topologyWindow);
        LOG.debug("ParsedRuleStr {}", parsedRuleStr);
        topologyWindow.setParsedRuleStr(parsedRuleStr);
        dao.add(topologyWindow);
        topologyWindow.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        return topologyWindow;
    }

    public TopologyWindow getWindow(Long topologyId, Long windowId) throws Exception {
        return getWindow(topologyId, windowId, getCurrentVersionId(topologyId));
    }

    public TopologyWindow getWindow(Long topologyId, Long windowId, Long versionId) throws Exception {
        TopologyWindow topologyTopologyWindow = new TopologyWindow();
        topologyTopologyWindow.setId(windowId);
        topologyTopologyWindow.setVersionId(versionId);
        TopologyWindow windowInfo = dao.get(new StorableKey(TOPOLOGY_WINDOWINFO_NAMESPACE, topologyTopologyWindow.getPrimaryKey()));
        if (windowInfo == null || !windowInfo.getTopologyId().equals(topologyId)) {
            return null;
        }
        windowInfo.setVersionTimestamp(getVersionTimestamp(versionId));
        return windowInfo;
    }

    public TopologyWindow addOrUpdateWindow(Long topologyId, Long windowId, TopologyWindow topologyWindow) throws Exception {
        Long currentTopologyVersionId = getCurrentVersionId(topologyId);
        topologyWindow.setId(windowId);
        topologyWindow.setVersionId(currentTopologyVersionId);
        topologyWindow.setTopologyId(topologyId);
        String parsedRuleStr = parseAndSerialize(topologyWindow);
        LOG.debug("ParsedRuleStr {}", parsedRuleStr);
        topologyWindow.setParsedRuleStr(parsedRuleStr);
        dao.addOrUpdate(topologyWindow);
        topologyWindow.setVersionTimestamp(updateVersionTimestamp(currentTopologyVersionId).getTimestamp());
        return topologyWindow;
    }

    public TopologyWindow removeWindow(Long topologyId, Long windowId) throws Exception {
        return removeWindow(topologyId, windowId, getCurrentVersionId(topologyId));
    }

    public TopologyWindow removeWindow(Long topologyId, Long windowId, Long versionId) throws Exception {
        TopologyWindow topologyWindow = getWindow(topologyId, windowId, versionId);
        if (topologyWindow != null) {
            topologyWindow = dao.remove(new StorableKey(TOPOLOGY_WINDOWINFO_NAMESPACE, topologyWindow.getPrimaryKey()));
            topologyWindow.setVersionTimestamp(updateVersionTimestamp(versionId).getTimestamp());
        }
        return topologyWindow;
    }

    private String parseAndSerialize(TopologyRule topologyRule) throws JsonProcessingException {
        Rule rule = new Rule();
        rule.setId(topologyRule.getId());
        rule.setName(topologyRule.getName());
        rule.setDescription(topologyRule.getDescription());
        rule.setWindow(topologyRule.getWindow());
        rule.setActions(topologyRule.getActions());

        if (topologyRule.getStreams() != null && !topologyRule.getStreams().isEmpty()) {
            topologyRule.setSql(getSqlString(topologyRule.getStreams(), topologyRule.getProjections(), topologyRule.getCondition(), null));
        } else if (StringUtils.isEmpty(topologyRule.getSql())) {
            throw new IllegalArgumentException("Either streams or sql string should be specified.");
        }
        updateRuleWithSql(rule, topologyRule.getSql(), topologyRule.getTopologyId(), topologyRule.getVersionId());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(rule);
    }

    private String parseAndSerialize(TopologyBranchRule ruleInfo) throws JsonProcessingException {
        Rule rule = new Rule();
        rule.setId(ruleInfo.getId());
        rule.setName(ruleInfo.getName());
        rule.setDescription(ruleInfo.getDescription());
        rule.setActions(ruleInfo.getActions());
        String sql = getSqlString(Collections.singletonList(ruleInfo.getStream()), null, ruleInfo.getCondition(), null);
        updateRuleWithSql(rule, sql, ruleInfo.getTopologyId(), ruleInfo.getVersionId());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(rule);
    }

    private String parseAndSerialize(TopologyWindow topologyWindow) throws JsonProcessingException {
        if (topologyWindow.getStreams() == null || topologyWindow.getStreams().isEmpty()) {
            LOG.error("Streams should be specified.");
            return StringUtils.EMPTY;
        }
        Rule rule = new Rule();
        rule.setId(topologyWindow.getId());
        rule.setName(topologyWindow.getName());
        rule.setDescription(topologyWindow.getDescription());
        rule.setWindow(topologyWindow.getWindow());
        rule.setActions(topologyWindow.getActions());
        String sql = getSqlString(topologyWindow.getStreams(),
                topologyWindow.getProjections(),
                topologyWindow.getCondition(),
                topologyWindow.getGroupbykeys());
        updateRuleWithSql(rule, sql, topologyWindow.getTopologyId(), topologyWindow.getVersionId());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(rule);
    }

    String getSqlString(List<String> streams,
                                List<Projection> projections,
                                String condition,
                                List<String> groupByKeys) {
        String SQL = select(streams, projections).orElse("SELECT * ");
        SQL += join(" FROM ", getTable(streams)).get();
        SQL += join(" WHERE ", convertNested(streams, condition)).orElse("");
        SQL += join(" GROUP BY ", convertNested(streams, groupByKeys)).orElse("");
        return SQL;
    }

    /*
      Converts streamline dot(.) separated nested expr to calcite format '[]'.
      Dot immediately following a stream name are retained.

      E.g. f1.g.h = 'A' and kafka_stream_1.f2[5].j = 100
      TO   f1['g']['h'] = 'A' and kafka_stream_1.f2[5]['j'] = 100

      In the above example 'kafka_stream_1' is the input stream and the dot immediately following it is retained.
     */
    String convertNested(List<String> streams, String expr) {
        if (StringUtils.isEmpty(expr)) {
            return expr;
        }
        StringBuilder sb = new StringBuilder();
        Pattern pattern = Pattern.compile("(\\w+)?\\.(\\w+)");
        Matcher matcher = pattern.matcher(expr);
        int startFrom = 0;
        int end = 0;
        while (matcher.find(startFrom)) {
            String prefix = matcher.group(1);
            String suffix = matcher.group(2);
            if (end < matcher.start()) {
                sb.append(expr.substring(end, matcher.start()));
            }
            if (streams.contains(prefix)) {
                sb.append(matcher.group());
            } else {
                if (startFrom == 0) {
                    sb.append(prefix);
                }
                sb.append("['").append(suffix).append("']");
            }
            startFrom = matcher.start(2);
            end = matcher.end();
        }
        sb.append(expr.substring(end));
        return sb.toString();
    }

    private List<String> convertNested(List<String> streams, List<String> exprs) {
        if (exprs == null || exprs.isEmpty()) {
            return exprs;
        }
        return exprs.stream().map(x -> convertNested(streams, x)).collect(Collectors.toList());
    }

    private Optional<String> select(List<String> streams, List<Projection> projections) {
        if (projections != null) {
            return join("SELECT ", projections.stream()
                    .map(p -> {
                        Projection res = new Projection(convertNested(streams, p.expr), p.functionName,
                                convertNested(streams, p.args), p.outputFieldName);
                        return res.toString();
                    })
                    .collect(Collectors.toList()));
        }
        return Optional.empty();
    }

    private Optional<String> join(String keyword, String part) {
        if (part != null) {
            return join(keyword, Collections.singletonList(part));
        }
        return Optional.empty();
    }

    private Optional<String> join(String keyword, Collection<String> parts) {
        if (parts != null && !parts.isEmpty()) {
            return Optional.of(keyword + Joiner.on(",").join(parts));
        }
        return Optional.empty();
    }

    private String getTable(List<String> streams) {
        if (streams == null || streams.isEmpty()) {
            LOG.warn("Rule condition is specified without input stream, will use default stream");
            return StreamlineEvent.DEFAULT_SOURCE_STREAM;
        } else if (streams.size() == 1) {
            return streams.iterator().next();
        } else {
            throw new UnsupportedOperationException("Joining multiple streams");
        }
    }

    private void updateRuleWithSql(Rule rule, String sql, Long topologyId, Long versionId) {
        // parse
        RuleParser ruleParser = new RuleParser(this, sql, topologyId, versionId);
        ruleParser.parse();

        // update rule with parsed sql constructs
        rule.setStreams(new HashSet<>(Collections2.transform(ruleParser.getStreams(), new Function<Stream, String>() {
            @Override
            public String apply(Stream input) {
                return input.getId();
            }
        })));
        rule.setProjection(ruleParser.getProjection());
        rule.setCondition(ruleParser.getCondition());
        rule.setGroupBy(ruleParser.getGroupBy());
        rule.setHaving(ruleParser.getHaving());
        rule.setReferredUdfs(ruleParser.getReferredUdfs());
    }


    public Collection<UDF> listUDFs() {
        return this.dao.list(UDF_NAMESPACE);
    }

    public Collection<UDF> listUDFs(List<QueryParam> queryParams) {
        return dao.find(UDF_NAMESPACE, queryParams);
    }

    public UDF getUDF(Long id) {
        UDF udf = new UDF();
        udf.setId(id);
        return this.dao.get(new StorableKey(UDF_NAMESPACE, udf.getPrimaryKey()));
    }

    public UDF addUDF(UDF udf) {
        if (udf.getId() == null) {
            udf.setId(this.dao.nextId(UDF_NAMESPACE));
        }
        udf.setName(udf.getName().toUpperCase());
        this.dao.add(udf);
        return udf;
    }

    public Map<String, Class<?>> loadUdfsFromJar(java.io.File jarFile) throws IOException {
        Map<String, Class<?>> udafs = new HashMap<>();

        for (Class<?> udfClass : UDF_CLASSES) {
            for (Class<?> clazz : ProxyUtil.loadAllClassesFromJar(jarFile, udfClass)) {
                udafs.put(clazz.getCanonicalName(), clazz);
            }
        }

        return udafs;
    }

    public UDF removeUDF(Long id) {
        UDF udf = new UDF();
        udf.setId(id);
        return dao.remove(new StorableKey(UDF_NAMESPACE, udf.getPrimaryKey()));
    }

    public UDF addOrUpdateUDF(Long udfId, UDF udf) {
        udf.setId(udfId);
        udf.setName(udf.getName().toUpperCase());
        this.dao.addOrUpdate(udf);
        return udf;
    }

    private void loadTransformationClassForBundle (TopologyComponentBundle topologyComponentBundle, InputStream bundleJar) {
        if (topologyComponentBundle.getStreamingEngine().equals(TopologyLayoutConstants.STORM_STREAMING_ENGINE)) {
            if (topologyComponentBundle.getBuiltin()) {
                // no transformation class validations for top level topology type
                if (topologyComponentBundle.getType() == TopologyComponentBundle.TopologyComponentType.TOPOLOGY) {
                    return;
                }
                try {
                    FluxComponent fluxComponent = (FluxComponent) Class.forName(topologyComponentBundle.getTransformationClass()).newInstance();
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    LOG.debug("Got exception", e);
                    throw new RuntimeException("Check transformationClass property. Cannot load builtin transformation class " + topologyComponentBundle
                            .getTransformationClass());
                }
            } else {
                ProxyUtil<FluxComponent> fluxComponentProxyUtil = new ProxyUtil<FluxComponent>(FluxComponent.class);
                OutputStream os = null;
                InputStream is = null;
                try {
                    java.io.File tmpFile = java.io.File.createTempFile(UUID.randomUUID().toString(), ".jar");
                    tmpFile.deleteOnExit();
                    os = new FileOutputStream(tmpFile);
                    ByteStreams.copy(bundleJar, os);
                    FluxComponent fluxComponent = fluxComponentProxyUtil.loadClassFromJar(tmpFile.getAbsolutePath(), topologyComponentBundle
                            .getTransformationClass());
                } catch (Exception ex) {
                    LOG.debug("Got exception", ex);
                    throw new RuntimeException("Cannot load transformation class " + topologyComponentBundle.getTransformationClass() + " from bundle Jar: ");
                } finally {
                    try {
                        if (os != null) {
                            os.close();
                        }
                    } catch (IOException ex) {
                        LOG.error("Got exception", ex);
                    }
                }
            }
        }
    }

    private String getTopologyComponentBundleJarName (TopologyComponentBundle topologyComponentBundle) {
        List<String> jarFileName = Arrays.asList(topologyComponentBundle.getStreamingEngine(), topologyComponentBundle.getType().name(), topologyComponentBundle
                .getSubType(), UUID.randomUUID().toString(), ".jar");
        String bundleJarFileName = String.join("-", jarFileName);
        return bundleJarFileName;
    }

    private Collection<File> listFiles(List<QueryParam> queryParams) {
        return dao.find(File.NAME_SPACE, queryParams);
    }
}
