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

import com.hortonworks.registries.storage.exception.IgnoreTransactionRollbackException;
import com.hortonworks.streamline.streams.actions.TopologyActions;
import com.hortonworks.streamline.streams.catalog.CatalogToLayoutConverter;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.exception.TopologyNotAliveException;
import com.hortonworks.streamline.streams.layout.component.TopologyDag;
import com.hortonworks.streamline.streams.layout.component.TopologyLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class captures the different states a 'topology' can be in and the state transitions.
 *
 * This follows the State pattern approach as described in GOF design patterns.
 */
public final class TopologyStates {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyStates.class);

    public static final TopologyState TOPOLOGY_STATE_INITIAL = new TopologyState() {
        @Override
        public void deploy(TopologyContext context) throws Exception {
            try {
                context.setCurrentAction("Constructing topology DAG");
                Topology topology = context.getTopology();
                TopologyDag dag = context.getTopologyActionsService().getTopologyDagBuilder().getDag(topology);
                topology.setTopologyDag(dag);
                context.setState(TOPOLOGY_STATE_DAG_CONSTRUCTED);
                context.setCurrentAction("DAG constructed");
            } catch (Exception ex) {
                context.setState(TOPOLOGY_STATE_DEPLOYMENT_FAILED);
                context.setCurrentAction("Topology DAG construction failed due to: " + ex);
                throw new IgnoreTransactionRollbackException(ex);
            }
        }
    };

    public static final TopologyState TOPOLOGY_STATE_DAG_CONSTRUCTED = new TopologyState() {
        @Override
        public void deploy(TopologyContext context) throws Exception {
            try {
                context.setCurrentAction("Validating topology DAG");
                TopologyDag dag = context.getTopology().getTopologyDag();
                context.getTopologyActionsService().ensureValid(dag);
                context.setState(TOPOLOGY_STATE_DAG_VALIDATED);
                context.setCurrentAction("Topology DAG validated");
            } catch (Exception ex) {
                context.setState(TOPOLOGY_STATE_DEPLOYMENT_FAILED);
                context.setCurrentAction("Topology DAG validation failed due to: " + ex);
                throw new IgnoreTransactionRollbackException(ex);
            }
        }
    };

    public static final TopologyState TOPOLOGY_STATE_DAG_VALIDATED = new TopologyState() {
        @Override
        public void deploy(TopologyContext context) throws Exception {
            try {
                context.setCurrentAction("Setting up cluster artifacts");
                Topology topology = context.getTopology();
                TopologyActions topologyActions = context.getTopologyActions();
                context.getTopologyActionsService().setUpClusterArtifacts(topology, topologyActions);
                context.setState(TOPOLOGY_STATE_CLUSTER_ARTIFACTS_SETUP);
                context.setCurrentAction("Cluster artifacts set up");
            } catch (Exception ex) {
                LOG.error("Error while setting up cluster artifacts", ex);
                context.setState(TOPOLOGY_STATE_DEPLOYMENT_FAILED);
                context.setCurrentAction("Cluster artifacts set up failed due to: " + ex);
                throw new IgnoreTransactionRollbackException(ex);
            }
        }
    };

    public static final TopologyState TOPOLOGY_STATE_CLUSTER_ARTIFACTS_SETUP = new TopologyState() {
        @Override
        public void deploy(TopologyContext context) throws Exception {
            try {
                context.setCurrentAction("Setting up extra jars");
                Topology topology = context.getTopology();
                TopologyActions topologyActions = context.getTopologyActions();
                String mavenArtifacts = context.getTopologyActionsService().setUpExtraJars(topology, topologyActions);
                context.setMavenArtifacts(mavenArtifacts);
                context.setState(TOPOLOGY_STATE_EXTRA_JARS_SETUP);
                context.setCurrentAction("Extra jars set up");
            } catch (Exception ex) {
                LOG.error("Error while setting up extra jars", ex);
                context.setState(TOPOLOGY_STATE_DEPLOYMENT_FAILED);
                context.setCurrentAction("Extra jars setup failed due to: " + ex);
                throw new IgnoreTransactionRollbackException(ex);
            }
        }
    };

    public static final TopologyState TOPOLOGY_STATE_EXTRA_JARS_SETUP = new TopologyState() {
        @Override
        public void deploy(TopologyContext context) throws Exception {
            TopologyActions topologyActions = context.getTopologyActions();
            Topology topology = context.getTopology();
            TopologyDag dag = topology.getTopologyDag();
            TopologyLayout layout = CatalogToLayoutConverter.getTopologyLayout(topology, dag);
            if (dag == null) {
                throw new IllegalStateException("Topology dag not set up");
            }
            try {
                context.setCurrentAction("Submitting topology to streaming engine");
                String mavenArtifacts = context.getMavenArtifacts();
                topologyActions.deploy(layout, mavenArtifacts, context, context.getAsUser());
                context.setState(TOPOLOGY_STATE_DEPLOYED);
                context.setCurrentAction("Topology deployed");
            } catch (Exception ex) {
                LOG.error("Error while trying to deploy the topology in the streaming engine", ex);
                LOG.error("Trying to kill any running instance of topology '{}'", context.getTopology().getName());
                killTopologyIfRunning(context, layout);
                context.setState(TOPOLOGY_STATE_DEPLOYMENT_FAILED);
                context.setCurrentAction("Topology submission failed due to: " + ex);
                throw new IgnoreTransactionRollbackException(ex);
            }
        }

        private void killTopologyIfRunning(TopologyContext context, TopologyLayout layout) {
            try {
                TopologyActions.Status engineStatus = context.getTopologyActions().status(layout, context.getAsUser());
                if (!engineStatus.getStatus().equals(TopologyActions.Status.STATUS_UNKNOWN)) {
                    invokeKill(context);
                }
            } catch (Exception e) {
                LOG.debug("Not able to get running status of topology '{}'", context.getTopology().getName());
            }
        }
    };

    public static final TopologyState TOPOLOGY_STATE_DEPLOYED = new TopologyState() {
        @Override
        public void kill(TopologyContext context) throws Exception {
            doKill(context);
        }

        @Override
        public void suspend(TopologyContext context) throws Exception {
            try {
                context.setCurrentAction("Suspending topology");
                Topology topology = context.getTopology();
                TopologyActions topologyActions = context.getTopologyActions();
                topologyActions.suspend(CatalogToLayoutConverter.getTopologyLayout(topology), context.getAsUser());
                context.setState(TOPOLOGY_STATE_SUSPENDED);
                context.setCurrentAction("Topology suspended");
            } catch (Exception ex) {
                LOG.error("Error while trying to suspend the topology", ex);
                context.setCurrentAction("Suspending the topology failed due to: " + ex);
                throw new IgnoreTransactionRollbackException(ex);
            }
        }
    };

    public static final TopologyState TOPOLOGY_STATE_SUSPENDED = new TopologyState() {
        @Override
        public void kill(TopologyContext context) throws Exception {
            doKill(context);
        }

        @Override
        public void resume(TopologyContext context) throws Exception {
            try {
                context.setCurrentAction("Resuming topology");
                Topology topology = context.getTopology();
                TopologyActions topologyActions = context.getTopologyActions();
                topologyActions.resume(CatalogToLayoutConverter.getTopologyLayout(topology), context.getAsUser());
                context.setState(TOPOLOGY_STATE_DEPLOYED);
                context.setCurrentAction("Topology resumed");
            } catch (Exception ex) {
                LOG.error("Error while trying to resume the topology", ex);
                context.setCurrentAction("Resuming the topology failed due to: " + ex);
                throw new IgnoreTransactionRollbackException(ex);
            }
        }
    };

    // deployment error state from where we can attempt to redeploy
    public static final TopologyState TOPOLOGY_STATE_DEPLOYMENT_FAILED = new TopologyState() {
        @Override
        public void deploy(TopologyContext context) throws Exception {
            context.setState(TOPOLOGY_STATE_INITIAL);
            context.setCurrentAction("Redeploying");
        }
    };

    private static void doKill(TopologyContext context) throws Exception {
        try {
            context.setCurrentAction("Killing topology");
            invokeKill(context);
            context.setState(TOPOLOGY_STATE_INITIAL);
            context.setCurrentAction("Topology killed");
        } catch (TopologyNotAliveException ex) {
            LOG.warn("Got TopologyNotAliveException while trying to kill topology, " +
                    "probably the topology was killed externally.");
            context.setState(TOPOLOGY_STATE_INITIAL);
            context.setCurrentAction("Setting topology to initial state since its not alive in the cluster");
        } catch (Exception ex) {
            LOG.error("Error while trying to kill the topology", ex);
            context.setCurrentAction("Killing the topology failed due to: " + ex);
            throw new IgnoreTransactionRollbackException(ex);
        }
    }

    private static void invokeKill(TopologyContext context) throws Exception {
        Topology topology = context.getTopology();
        TopologyActions topologyActions = context.getTopologyActions();
        topologyActions.kill(CatalogToLayoutConverter.getTopologyLayout(topology), context.getAsUser());
        LOG.debug("Killed topology '{}'", topology.getName());
    }
}
