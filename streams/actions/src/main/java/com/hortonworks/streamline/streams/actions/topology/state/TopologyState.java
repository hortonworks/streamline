package com.hortonworks.streamline.streams.actions.topology.state;

public abstract class TopologyState {
    public void deploy(TopologyContext context) throws Exception {
        throw new IllegalStateException("Invalid action for current state: " + this);
    }

    public void kill(TopologyContext context) throws Exception {
        throw new IllegalStateException("Invalid action for current state: " + this);
    }

    public void suspend(TopologyContext context) throws Exception {
        throw new IllegalStateException("Invalid action for current state: " + this);
    }

    public void resume(TopologyContext context) throws Exception {
        throw new IllegalStateException("Invalid action for current state: " + this);
    }

    @Override
    public String toString() {
        return TopologyStateFactory.getInstance().getTopologyStateName(this);
    }
}
