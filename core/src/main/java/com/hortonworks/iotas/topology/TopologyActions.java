package com.hortonworks.iotas.topology;

import com.hortonworks.iotas.catalog.Topology;

import java.util.Map;

/**
 * Interface representing options that need to be supported on a topology
 * layout once its created using the UI.
 */
public interface TopologyActions {
    // Any one time initialization is done here
    void init (Map<String, String> conf);

    // Deploy the artifact generated using the underlying streaming
    // engine
    void deploy (Topology topology) throws Exception;

    //Kill the artifact that was deployed using deploy
    void kill (Topology topology) throws Exception;

    //Validate the json representing the IoTaS based on underlying streaming
    // engine
    void validate (Topology topology) throws Exception;

    //Suspend the json representing the IoTaS based on underlying streaming
    // engine
    void suspend (Topology topology) throws Exception;

    //Resume the json representing the IoTaS based on underlying streaming
    // engine
    void resume (Topology topology) throws Exception;

    // return topology status
    Status status (Topology topology) throws Exception;

    interface Status {
        String getStatus();
        Map<String, String> getExtra();
    }
}
