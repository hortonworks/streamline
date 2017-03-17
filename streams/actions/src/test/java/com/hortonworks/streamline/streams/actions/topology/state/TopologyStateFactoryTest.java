package com.hortonworks.streamline.streams.actions.topology.state;

import static org.junit.Assert.*;

public class TopologyStateFactoryTest {
    @org.junit.Test (expected = IllegalArgumentException.class)
    public void getTopologyStateInvalid() throws Exception {
        TopologyStateFactory topologyStateFactory = TopologyStateFactory.getInstance();
        topologyStateFactory.getTopologyState("TEST");
    }

    @org.junit.Test
    public void getTopologyStateValid() throws Exception {
        TopologyStateFactory topologyStateFactory = TopologyStateFactory.getInstance();
        TopologyState state = topologyStateFactory.getTopologyState("TOPOLOGY_STATE_INITIAL");
        assertEquals(state, TopologyStates.TOPOLOGY_STATE_INITIAL);
    }
}