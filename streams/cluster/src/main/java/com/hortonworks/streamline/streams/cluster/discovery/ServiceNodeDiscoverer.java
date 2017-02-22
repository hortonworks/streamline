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
package com.hortonworks.streamline.streams.cluster.discovery;

import com.hortonworks.streamline.streams.exception.ConfigException;

import java.util.List;
import java.util.Map;

/**
 * Interface for service and node discovery.
 */
public interface ServiceNodeDiscoverer {
  /**
   * Initialize method. Any one time initialization is done here.
   *
   * @param conf Configuration for implementation of ServiceNodeDiscoverer.
   * @throws ConfigException throw when instance can't be initialized with this configuration (misconfigured).
   */
  void init(Map<String, String> conf) throws ConfigException;

  /**
   * Get all service names from cluster.
   *
   * @return all service names from cluster. some examples are 'STORM', 'KAFKA', and so on.
   */
  List<String> getServices();

  /**
   * Get all component names from cluster for given service.
   *
   * @param serviceName service name. some examples are 'STORM', 'KAFKA', and so on.
   * @return all component names for given service. some examples are 'NIMBUS', 'UI SERVER', and so on.
   */
  List<String> getComponents(String serviceName);

  /**
   * Retrieves all hosts from cluster for given service.
   *
   * @param serviceName service name. some examples are 'STORM', 'KAFKA', and so on.
   * @param componentName component name. some examples are 'NIMBUS', 'UI SERVER', and so on.
   * @return all hosts for given component in service.
   */
  List<String> getComponentNodes(String serviceName, String componentName);

  /**
   * Retrieves all configurations from cluster for given service.
   * Note that this should return the map which is associating config type -> config key -> config value.
   *
   * @param serviceName service name. some examples are 'STORM', 'KAFKA', and so on.
   * @return all configurations for given service.
   */
  Map<String, Map<String, String>> getConfigurations(String serviceName);

  /**
   * Get original file name for given config type. This is for including configuration files to topology jar.
   *
   * @param configType config type which is as same as discoverer provides
   * @return original configuration file name
   */
  String getOriginalFileName(String configType);
}
