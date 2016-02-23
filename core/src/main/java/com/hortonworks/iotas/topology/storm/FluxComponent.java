package com.hortonworks.iotas.topology.storm;

import com.hortonworks.iotas.util.exception.BadTopologyLayoutException;

import java.util.List;
import java.util.Map;

/**
 * An interface to be implemented by different storm components like spout,
 * bolt, streams to return the flux yaml equivalent of the component given a
 * configuration
 */
public interface FluxComponent {
    /*
    Initialize the implementation with catalog root url
     */
    void withCatalogRootUrl (String catalogRootUrl);
    /*
    Method to initialize the implementation with a configuration
     */
    void withConfig (Map<String, Object> config);

    /*
    Get yaml maps of all the components referenced by this component
    Expected to return equivalent of something like below.
    - id: "zkHosts"
    className: "org.apache.storm.kafka.ZkHosts"
    constructorArgs:
      - ${kafka.spout.zkUrl}

    - id: "spoutConfig"
    className: "org.apache.storm.kafka.SpoutConfig"
    constructorArgs:
      - ref: "zkHosts"
     */
    List<Map<String, Object>> getReferencedComponents ();

    /*
    Get yaml map for this component. Note that the id field will be
    overwritten and hence is optional.
    Expected to return equivalent of something like below
    - id: "KafkaSpout"
    className: "org.apache.storm.kafka.KafkaSpout"
    constructorArgs:
      - ref: "spoutConfig"
     */
    Map<String, Object> getComponent ();

    /*
    validate the configuration for this component.
    throw BadTopologyLayoutException if configuration is not correct
     */
    void validateConfig () throws BadTopologyLayoutException;
}
