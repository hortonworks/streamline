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
package com.hortonworks.streamline.streams.runtime;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.exception.ConfigException;
import com.hortonworks.streamline.streams.exception.ProcessingException;

import java.util.List;
import java.util.Map;

/**
 * An interface for supporting custom processor components in an Streamline topology
 */
public interface CustomProcessorRuntime {

    /**
     * Validate configuration provided and throw a {@link ConfigException} if missing or invalid configuration
     * @throws ConfigException
     * @param config
     */
    void validateConfig(Map<String, Object> config) throws ConfigException;

    /**
     * Process the {@link StreamlineEvent} and throw a {@link ProcessingException} if an error arises during processing
     * @param event to be processed
     * @return List of events to be emitted for the input streamline event adhering to the output schema defined while registering the CP implementation
     * @throws ProcessingException
     */
    List<StreamlineEvent> process (StreamlineEvent event) throws ProcessingException;

    /**
     * Initialize any necessary resources needed for the implementation
     * @param config
     */
    void initialize(Map<String, Object> config);

    /**
     * Clean up any necessary resources needed for the implementation
     */
    void cleanup();

}
