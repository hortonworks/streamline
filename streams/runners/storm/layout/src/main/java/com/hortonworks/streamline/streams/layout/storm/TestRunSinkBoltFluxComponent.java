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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSink;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TestRunSinkBoltFluxComponent extends AbstractFluxComponent {
    private final Logger log = LoggerFactory.getLogger(TestRunSinkBoltFluxComponent.class);

    protected TestRunSink testRunSink;

    @Override
    protected void generateComponent() {
        testRunSink = (TestRunSink) conf.get(StormTopologyLayoutConstants.STREAMLINE_COMPONENT_CONF_KEY);
        String boltId = "testRunSinkBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "com.hortonworks.streamline.streams.runtime.storm.testing.TestRunSinkBolt";
        List<Object> constructorArgs = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        String testRunSinkJson = null;
        try {
            testRunSinkJson = mapper.writeValueAsString(testRunSink);
        } catch (JsonProcessingException e) {
            log.error("Error creating json config string for TestRunSink", e);
        }
        constructorArgs.add(testRunSinkJson);
        component = createComponent(boltId, boltClassName, null, constructorArgs, null);
        addParallelismToComponent();
    }

}
