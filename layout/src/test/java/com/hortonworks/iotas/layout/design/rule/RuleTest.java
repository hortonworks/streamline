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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hortonworks.iotas.layout.design.rule;

import com.hortonworks.iotas.layout.design.component.RulesProcessor;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.junit.Test;

public class RuleTest {

    @Test
    public void testBuildRuleProcessor() throws Exception {
        final RulesProcessor rulesProcessorMock = new RuleProcessorMockBuilder(1, 2, 2).build();

        //JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(Feature.FAIL_ON_EMPTY_BEANS, false);
        String ruleProcessorJson = mapper.writeValueAsString(rulesProcessorMock);
        System.out.println(ruleProcessorJson);
        //TODO
//        mapper.registerSubtypes(FieldConditionElement.class);
//        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
//        RulesProcessor<Schema, Schema, Field> rulesProcessor1 = mapper.readValue(ruleProcessorJson, RulesProcessor.class);
//        System.out.println(rulesProcessorMock);
//        System.out.println(rulesProcessor1);
    }

}