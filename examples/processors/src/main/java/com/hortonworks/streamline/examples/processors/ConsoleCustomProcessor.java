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
package com.hortonworks.streamline.examples.processors;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.Result;
import com.hortonworks.streamline.streams.exception.ConfigException;
import com.hortonworks.streamline.streams.exception.ProcessingException;
import com.hortonworks.streamline.streams.runtime.CustomProcessorRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Console Processor is a sample custom processor to test the storm topology with custom processor bolt
 */
public class ConsoleCustomProcessor implements CustomProcessorRuntime {
    protected static final Logger LOG = LoggerFactory.getLogger(ConsoleCustomProcessor.class);
    public static final String CONFIG_FIELD_NAME = "configField";
    Map<String, Object> config = new HashMap<>();
 
    public void initialize(Map<String, Object> config) {
        if (config != null) {
            this.config = config;
        }
        LOG.info("Initializing with config field " + CONFIG_FIELD_NAME + " = " + this.config.get(CONFIG_FIELD_NAME));
    }

    
    public void validateConfig(Map<String, Object> config) throws ConfigException {
        LOG.debug("Validating config ");
        if (!config.containsKey(CONFIG_FIELD_NAME)) {
            throw new ConfigException("Missing config field: " + CONFIG_FIELD_NAME);
        }
        LOG.debug("Config valid ");
    }


    public List<Result> process(StreamlineEvent event) throws ProcessingException {
        LOG.info("Processing {}", event);
        List<Result> results = new ArrayList<>();
        results.add(new Result("stream1",Arrays.asList(event)));
        return results;
    }

   
    public void cleanup() {
        LOG.debug("Cleaning up");
    }
}
