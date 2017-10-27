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
import com.hortonworks.streamline.streams.layout.component.impl.testing.TestRunRulesProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestRunRulesProcessorBoltFluxComponent extends AbstractFluxComponent {
    private final Logger log = LoggerFactory.getLogger(TestRunRulesProcessorBoltFluxComponent.class);

    protected TestRunRulesProcessor testRunRulesProcessor;

    @Override
    protected void generateComponent() {
        testRunRulesProcessor = (TestRunRulesProcessor) conf.get(StormTopologyLayoutConstants.STREAMLINE_COMPONENT_CONF_KEY);
        String boltId = "testRunRulesProcessorBolt" + UUID_FOR_COMPONENTS;

        String boltClassName;
        if (testRunRulesProcessor.isWindowed()) {
            boltClassName = "com.hortonworks.streamline.streams.runtime.storm.testing.TestRunWindowProcessorBolt";
        } else {
            boltClassName = "com.hortonworks.streamline.streams.runtime.storm.testing.TestRunProcessorBolt";
        }

        String underlyingBoltComponentId = addUnderlyingBoltComponent();

        List<Object> constructorArgs = new ArrayList<>();
        constructorArgs.add(getRefYaml(underlyingBoltComponentId));
        constructorArgs.add(testRunRulesProcessor.getEventLogFilePath());
        component = createComponent(boltId, boltClassName, null, constructorArgs, null);
        addParallelismToComponent();
    }

    private String addUnderlyingBoltComponent() {
        StreamlineProcessor underlyingProcessor = testRunRulesProcessor.getUnderlyingProcessor();
        String transformationClass = underlyingProcessor.getTransformationClass();
        Map<String, Object> underlyingComponent;
        try {
            AbstractFluxComponent transformation = ReflectionHelper.newInstance(transformationClass);

            Map<String, Object> props = new LinkedHashMap<>();
            props.putAll(underlyingProcessor.getConfig().getProperties());
            props.put(StormTopologyLayoutConstants.STREAMLINE_COMPONENT_CONF_KEY, testRunRulesProcessor.getUnderlyingProcessor());
            // should get rid of below things which is only supported to spout and bolt in flux
            props.remove("parallelism");
            transformation.withConfig(props);
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
