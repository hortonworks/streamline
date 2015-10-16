package com.hortonworks.iotas.util;

import com.hortonworks.iotas.catalog.DataStream;

import java.io.IOException;
import java.util.Map;

/**
 * Interface representing options that need to be supported on a data stream
 * layout once its created using the UI.
 */
public interface DataStreamActions {
    // Any one time initialization is done here
    public void init (Map<String, String> conf);


    // Deploy the artifact generated using the underlying streaming
    // engine
    public void deploy (DataStream dataStream) throws Exception;

    //Kill the artifact that was deployed using deploy
    public void kill (DataStream dataStream) throws Exception;
}
