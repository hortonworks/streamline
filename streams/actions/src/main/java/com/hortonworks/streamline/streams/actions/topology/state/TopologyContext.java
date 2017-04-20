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
package com.hortonworks.streamline.streams.actions.topology.state;

import com.hortonworks.streamline.streams.actions.TopologyActionContext;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class TopologyContext implements TopologyActionContext {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyContext.class);

    private final Topology topology;
    private final TopologyActionsService topologyActionsService;
    private String mavenArtifacts;
    private TopologyState state;
    private String asUser;

    public TopologyContext(Topology topology, TopologyActionsService actionsService) {
        this(topology, actionsService, TopologyStates.TOPOLOGY_STATE_INITIAL, null);
    }

    public TopologyContext(Topology topology, TopologyActionsService actionsService, TopologyState state, String asUser) {
        Objects.requireNonNull(topology, "null topology");
        Objects.requireNonNull(actionsService, "null actionsService");
        Objects.requireNonNull(state, "null state");
        this.topology = topology;
        this.topologyActionsService = actionsService;
        this.state = state;
        this.asUser = asUser;
    }

    public TopologyState getState() {
        return state;
    }

    public void setState(TopologyState state) {
        this.state = state;
    }

    public String getStateName() {
        return TopologyStateFactory.getInstance().getTopologyStateName(state);
    }

    public TopologyActionsService getTopologyActionsService() {
        return topologyActionsService;
    }

    public TopologyActions getTopologyActions() {
        return topologyActionsService.getTopologyActionsInstance(topology);
    }

    public Topology getTopology() {
        return topology;
    }

    public String getMavenArtifacts() {
        return mavenArtifacts;
    }

    public void setMavenArtifacts(String mavenArtifacts) {
        this.mavenArtifacts = mavenArtifacts;
    }

    public String getAsUser() {
        return asUser;
    }

    public void deploy() throws Exception {
        state.deploy(this);
    }

    public void kill() throws Exception {
        state.kill(this);
    }

    public void suspend() throws Exception {
        state.suspend(this);
    }

    public void resume() throws Exception {
        state.resume(this);
    }

    @Override
    public void setCurrentAction(String description) {
        com.hortonworks.streamline.streams.catalog.topology.state.TopologyState catalogState =
                new com.hortonworks.streamline.streams.catalog.topology.state.TopologyState();
        catalogState.setName(getStateName());
        catalogState.setTopologyId(topology.getId());
        catalogState.setDescription(description);
        LOG.debug("Topology id: {}, state: {}", topology.getId(), catalogState);
        topologyActionsService.getCatalogService().addOrUpdateTopologyState(topology.getId(), catalogState);
    }
}
