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

    // Generate the artifact based on the underlying streaming engine
    public void createDataStreamArtifact (DataStream dataStream) throws IOException;

    // Submit or deploy the artifact generated using the underlying streaming
    // engine
    //public void submitDataStreamArtifact (Long dataStreamId);

    //Kill the artifact that was submitted using submitDataStreamArtifact
    //public void killDataStreamArtifact (Long dataStreamId);
}
