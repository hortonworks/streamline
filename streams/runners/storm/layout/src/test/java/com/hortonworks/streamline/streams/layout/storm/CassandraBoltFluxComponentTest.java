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
package com.hortonworks.streamline.streams.layout.storm;

import org.apache.calcite.avatica.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.calcite.avatica.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public class CassandraBoltFluxComponentTest {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraBoltFluxComponentTest.class);

    private static final String FIELD_SELECTOR_CLASS = "com.hortonworks.streamline.streams.runtime.storm.cassandra.StreamlineFieldSelector";
    private static final String MAPPER_CLASS = "org.apache.storm.cassandra.query.builder.BoundCQLStatementMapperBuilder";

    @Test
    public void testCassandraFluxGeneration() throws Exception {
        CassandraBoltFluxComponent cassandraBoltFluxComponent = new CassandraBoltFluxComponent();
        InputStream resourceAsStream = this.getClass().getResourceAsStream("/cassandra-flux.json");
        Map<String, Object> config = new ObjectMapper().readValue(IOUtils.toString(resourceAsStream, "UTF-8"),
                                                                  new TypeReference<Map<String, Object>>() {});
        LOG.info("config = " + config);
        cassandraBoltFluxComponent.withConfig(config);

        FluxBoltGenerator fluxBoltGenerator = new FluxBoltGenerator();
        Map<String, Object> yamlMap = fluxBoltGenerator.generateYaml(cassandraBoltFluxComponent);
        String yamlString = fluxBoltGenerator.toYamlString(yamlMap);
        LOG.info("yamlString = \n" + yamlString);

        List<Map<String, Object>> components = (List<Map<String, Object>>) yamlMap.get(StormTopologyLayoutConstants.YAML_KEY_COMPONENTS);

        List<String> selectorIds = new ArrayList<>();
        List<Object> mapperConfigMethods = null;
        for (Map<String, Object> component : components) {
            String className = (String) component.get(StormTopologyLayoutConstants.YAML_KEY_CLASS_NAME);
            if(FIELD_SELECTOR_CLASS.equals(className)) {
                selectorIds.add((String) component.get(StormTopologyLayoutConstants.YAML_KEY_ID));
            } else if(MAPPER_CLASS.equals(className)) {
                mapperConfigMethods = (List<Object>) component.get(StormTopologyLayoutConstants.YAML_KEY_CONFIG_METHODS);
            }
        }

        // given columns should be converted to field selectors
        Assert.assertEquals(((List<Object>) config.get("columns")).size(), selectorIds.size());

        Assert.assertTrue(mapperConfigMethods.size() == 1);
        Map<String, Object> bindMethodConfig = (Map<String, Object>) mapperConfigMethods.iterator().next();

        // only one config method with name 'bind'
        Assert.assertEquals("bind", bindMethodConfig.get("name"));
        List<Map<String, Object>> args = (List<Map<String, Object>>) bindMethodConfig.get("args");
        Assert.assertEquals(1, args.size());
        List<String> bindArgRefs = (List<String>) args.get(0).get("reflist");

        // bind method args should be same as field selectors that are configured
        Assert.assertEquals(selectorIds, bindArgRefs);
    }

}
