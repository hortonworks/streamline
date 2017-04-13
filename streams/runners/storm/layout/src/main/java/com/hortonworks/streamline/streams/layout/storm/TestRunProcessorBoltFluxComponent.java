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

import com.hortonworks.streamline.common.util.ReflectionHelper;
import com.hortonworks.streamline.streams.layout.component.StreamlineProcessor;
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TestRunProcessorBoltFluxComponent extends AbstractFluxComponent {
    private final Logger log = LoggerFactory.getLogger(TestRunProcessorBoltFluxComponent.class);

    protected TestRunProcessor testRunProcessor;

    @Override
    protected void generateComponent() {
        testRunProcessor = (TestRunProcessor) conf.get(StormTopologyLayoutConstants.STREAMLINE_COMPONENT_CONF_KEY);
        String boltId = "testRunProcessorBolt" + UUID_FOR_COMPONENTS;

        String boltClassName;
        if (testRunProcessor.isWindowed()) {
            boltClassName = "com.hortonworks.streamline.streams.runtime.storm.testing.TestRunWindowProcessorBolt";
        } else {
            boltClassName = "com.hortonworks.streamline.streams.runtime.storm.testing.TestRunProcessorBolt";
        }

        String underlyingBoltComponentId = addUnderlyingBoltComponent();

        List<Object> constructorArgs = new ArrayList<>();
        constructorArgs.add(testRunProcessor.getName());
        constructorArgs.add(getRefYaml(underlyingBoltComponentId));
        constructorArgs.add(testRunProcessor.getEventLogFilePath());
        component = createComponent(boltId, boltClassName, null, constructorArgs, null);
        addParallelismToComponent();
    }

    private String addUnderlyingBoltComponent() {
        StreamlineProcessor underlyingProcessor = testRunProcessor.getUnderlyingProcessor();
        String transformationClass = underlyingProcessor.getTransformationClass();
        Map<String, Object> underlyingComponent;
        try {
            AbstractFluxComponent transformation = ReflectionHelper.newInstance(transformationClass);
            Map<String, Object> underlyingConf = Collections.singletonMap(StormTopologyLayoutConstants.STREAMLINE_COMPONENT_CONF_KEY, testRunProcessor.getUnderlyingProcessor());
            transformation.withConfig(underlyingConf);
            underlyingComponent = transformation.getComponent();

            for (Map<String, Object> dependentComponents : transformation.getReferencedComponents()) {
                addToComponents(dependentComponents);
            }

            addToComponents(underlyingComponent);

            return (String) underlyingComponent.get(StormTopologyLayoutConstants.YAML_KEY_ID);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            log.error("Error creating underlying transformation instance", e);
            throw new RuntimeException(e);
        }
    }
}
