/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.streamline.streams.layout.storm;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.streams.layout.component.impl.model.ModelProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class MLModelEvaluationFluxComponent extends AbstractFluxComponent {
    private static final Logger LOG = LoggerFactory.getLogger(MLModelEvaluationFluxComponent.class);

    private String modelOutputsComponentId;

    @Override
    protected void generateComponent() {
        LOG.debug("Generating [{}]", this.getClass().getSimpleName());
        final String boltId = "PMMLPredictorBolt" + UUID_FOR_COMPONENTS;
        final String boltClassName = "org.apache.storm.pmml.PMMLPredictorBolt";
        final List<Object> boltConstructorArgs = Lists.newArrayList(
                getRefYaml(modelRunnerFactory()),
                getRefYaml(modelOutputs()));

        component = createComponent(boltId, boltClassName, null, boltConstructorArgs, null);
        addParallelismToComponent();
        LOG.debug("SUCCESSFULLY generated [{}]", this.getClass().getSimpleName());
    }

    private String modelRunnerFactory() {
        final String componentId = "ModelRunnerFactory_" + UUID_FOR_COMPONENTS;
        final String className = "com.hortonworks.streamline.streams.runtime.storm.bolt.model.StreamlineJPMMLModelRunnerFactory";
        final List<Object> constructorArgs = Lists.newArrayList(modelProcessorJson(), getRefYaml(modelOutputs()));

        addToComponents(createComponent(componentId, className, null, constructorArgs, null));
        LOG.debug("Created [{}] with component id [{}]", "StreamlineJPMMLModelRunnerFactory", componentId);
        return componentId;
    }

    private String modelOutputs() {
        if (modelOutputsComponentId == null) {
            modelOutputsComponentId = "ModelOutputs_" + UUID_FOR_COMPONENTS;
            final String className = "com.hortonworks.streamline.streams.runtime.storm.bolt.model.StreamlineEventModelOutputs";
            final List<Object> constructorArgs = Collections.singletonList(modelProcessorJson());

            addToComponents(createComponent(modelOutputsComponentId, className, null, constructorArgs, null));
        }
        LOG.debug("Created [{}] with component id [{}]", "StreamlineEventModelOutputs", modelOutputsComponentId);
        return modelOutputsComponentId;
    }


    private String modelProcessorJson() {
        final ModelProcessor modelProcessorUI = (ModelProcessor) conf.get(StormTopologyLayoutConstants.STREAMLINE_COMPONENT_CONF_KEY);

        ObjectMapper mapper = new ObjectMapper();
        String modelProcessorJson;
        try {
            modelProcessorJson = mapper.writeValueAsString(modelProcessorUI);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error creating JSON config string for ML ModelProcessor", e);
        }

        LOG.debug("Created ML ModelProcessor JSON config [{}]", modelProcessorJson);
        return  modelProcessorJson;
    }
}
