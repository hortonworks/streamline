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
package org.apache.streamline.streams.layout.storm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.streamline.streams.layout.component.impl.model.ModelProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MLModelEvaluationFluxComponent extends AbstractFluxComponent {
    private static final Logger LOG = LoggerFactory.getLogger(MLModelEvaluationFluxComponent.class);

    @Override
    protected void generateComponent (){
        ModelProcessor modelProcessor = (ModelProcessor) conf.get(StormTopologyLayoutConstants.STREAMLINE_COMPONENT_CONF_KEY);
        String boltId = "modelEvaluationBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "org.apache.streamline.streams.runtime.storm.bolt.model.PMMLModelEvaluationBolt";
        List constructorArgs = new ArrayList<>();
        String modelProcessorJson = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            modelProcessorJson = mapper.writeValueAsString(modelProcessor);
        } catch (JsonProcessingException e) {
            LOG.error("Error creating json config string for NormalizationProcessor", e);
            throw new RuntimeException(e);
        }
        constructorArgs.add(modelProcessorJson);
        component = createComponent(boltId, boltClassName, null, constructorArgs, null);
        addParallelismToComponent();
    }
}
