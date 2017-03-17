package com.hortonworks.streamline.streams.actions;

/**
 * Context information that can be passed to the topology actions and for topology
 * actions to pass status back to the framework.
 */
public interface TopologyActionContext {
    /**
     * Set the current action thats is being performed. This may be used to show the details
     * of the current step being executed.
     *
     * @param description the description
     */
    void setCurrentAction(String description);
}
