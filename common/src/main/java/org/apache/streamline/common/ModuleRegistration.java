package org.apache.streamline.common;

import org.apache.streamline.common.util.FileStorage;

import java.util.List;
import java.util.Map;

/**
 * An interface expected to be implemented by indepenedent modules so that they can be registered with web service module on startup
 */
public interface ModuleRegistration {

    /**
     *
     * @param config module specific config from the yaml file
     * @param fileStorage file storage implementation that iotas is initialized with for the module to use if at all
     */
    void init (Map<String, Object> config, FileStorage fileStorage);

    /**
     *
     * @return list of resources to register with the web service module to handle end points for this module
     */
    List<Object> getResources ();
}
