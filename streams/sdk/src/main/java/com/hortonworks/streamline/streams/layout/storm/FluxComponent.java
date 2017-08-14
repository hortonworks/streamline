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
package com.hortonworks.streamline.streams.layout.storm;

import com.hortonworks.streamline.common.exception.ComponentConfigException;

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
    throw ComponentConfigException if configuration is not correct
     */
    void validateConfig () throws ComponentConfigException;
}
