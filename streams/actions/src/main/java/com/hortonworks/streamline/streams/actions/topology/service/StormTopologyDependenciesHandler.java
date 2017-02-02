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

import com.google.common.base.Joiner;
import com.hortonworks.streamline.common.ComponentTypes;
import com.hortonworks.streamline.common.QueryParam;
import com.hortonworks.streamline.streams.catalog.UDF;
import com.hortonworks.streamline.streams.catalog.processor.CustomProcessorInfo;
import com.hortonworks.streamline.streams.catalog.topology.TopologyComponentBundle;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.layout.component.Edge;
import com.hortonworks.streamline.streams.layout.component.StreamlineComponent;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.StreamlineSink;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import com.hortonworks.streamline.streams.layout.component.TopologyDagVisitor;
import com.hortonworks.streamline.streams.layout.component.impl.RulesProcessor;
import com.hortonworks.streamline.streams.layout.component.rule.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This visitor collects all the dependencies like extra jars, maven-artifacts etc for a topology.
 */
public class StormTopologyDependenciesHandler extends TopologyDagVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(StormTopologyDependenciesHandler.class);
    private final Set<String> extraJars = new HashSet<>();
    private final Set<String> resourceNames = new HashSet<>();
    private Set<TopologyComponentBundle> topologyComponentBundleSet = new HashSet<>();
    private List<String> mavenArtifacts = new ArrayList<>();

    private final StreamCatalogService catalogService;

    StormTopologyDependenciesHandler(StreamCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public void visit(RulesProcessor rulesProcessor) {
        Set<UDF> udfsToShip = new HashSet<>();
        for (Rule rule : rulesProcessor.getRules()) {
            for (String udf : rule.getReferredUdfs()) {
                Collection<UDF> udfs = catalogService.listUDFs(Collections.singletonList(new QueryParam(UDF.NAME, udf)));
                if (udfs.size() > 1) {
                    throw new IllegalArgumentException("Multiple UDF definitions with name:" + udf);
                } else if (udfs.size() == 1) {
                    udfsToShip.add(udfs.iterator().next());
                }
            }
            for (UDF udf : udfsToShip) {
                extraJars.add(udf.getJarStoragePath());
            }
        }
        resourceNames.addAll(rulesProcessor.getExtraResources());
        handleBundleForStreamlineComponent(rulesProcessor);
    }

    @Override
    public void visit(Edge edge) {
    }

    @Override
    public void visit(StreamlineSource source) {
        handleStreamlineComponent(source);
    }

    @Override
    public void visit(StreamlineSink sink) {
        handleStreamlineComponent(sink);
    }

    @Override
    public void visit(StreamlineProcessor processor) {
        handleStreamlineComponent(processor);
    }

    public Set<String> getExtraJars() {
        return extraJars;
    }

    public Set<String> getExtraResources() {
        return resourceNames;
    }

    public Set<TopologyComponentBundle>  getTopologyComponentBundleSet () {
        return topologyComponentBundleSet;
    }

    public String getMavenDeps () {
        return Joiner.on(",").join(mavenArtifacts);
    }

    private void handleStreamlineComponent (StreamlineComponent streamlineComponent) {
        extraJars.addAll(streamlineComponent.getExtraJars());
        resourceNames.addAll(streamlineComponent.getExtraResources());
        handleBundleForStreamlineComponent(streamlineComponent);
    }

    private void handleBundleForStreamlineComponent (StreamlineComponent streamlineComponent) {
        TopologyComponentBundle topologyComponentBundle = catalogService.getTopologyComponentBundle(Long.parseLong(streamlineComponent.
                getTopologyComponentBundleId()));
        if (topologyComponentBundle == null) {
            throw new RuntimeException("Likely to run in to issues while deployging topology since TopologyComponentBundle not found for id " +
                    streamlineComponent.getTopologyComponentBundleId());
        }
        if (!topologyComponentBundle.getBuiltin()) {
            topologyComponentBundleSet.add(topologyComponentBundle);
        } else {
            LOG.debug("No need to copy any jar for {} since its a builtin bundle", streamlineComponent);
        }
        if (topologyComponentBundle.getMavenDeps() != null) {
            mavenArtifacts.add(topologyComponentBundle.getMavenDeps());
        }
        if (TopologyComponentBundle.TopologyComponentType.PROCESSOR.equals(topologyComponentBundle.getType()) && ComponentTypes.CUSTOM.equals
                (topologyComponentBundle.getSubType())) {
            try {
                extraJars.add(new CustomProcessorInfo().fromTopologyComponentBundle(topologyComponentBundle).getJarFileName());
            } catch (IOException e) {
                LOG.warn("IOException while getting jar file name for custom processor from bundle", topologyComponentBundle);
                throw new RuntimeException(e);
            }
        }
    }
}
