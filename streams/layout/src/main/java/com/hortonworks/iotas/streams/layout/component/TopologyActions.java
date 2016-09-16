package com.hortonworks.iotas.streams.layout.component;

import java.nio.file.Path;
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
    void deploy (TopologyLayout topology) throws Exception;

    //Kill the artifact that was deployed using deploy
    void kill (TopologyLayout topology) throws Exception;

    //Validate the json representing the IoTaS based on underlying streaming
    // engine
    void validate (TopologyLayout topology) throws Exception;

    //Suspend the json representing the IoTaS based on underlying streaming
    // engine
    void suspend (TopologyLayout topology) throws Exception;

    //Resume the json representing the IoTaS based on underlying streaming
    // engine
    void resume (TopologyLayout topology) throws Exception;

    // return topology status
    Status status (TopologyLayout topology) throws Exception;

    /**
     * the Path where topology specific artifacts are kept
     */
    Path getArtifactsLocation(TopologyLayout topology);

    /**
     * the Path where extra jars to be deployed are kept
     */
    Path getExtraJarsLocation(TopologyLayout topology);

    interface Status {
        String getStatus();
        Map<String, String> getExtra();
    }
}
