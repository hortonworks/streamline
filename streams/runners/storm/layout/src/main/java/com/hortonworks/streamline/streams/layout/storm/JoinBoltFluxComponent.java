/**
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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.DumperOptions;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;

import com.hortonworks.streamline.streams.layout.component.rule.expression.Window;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/* ---- Sample Json of whats expected from UI  ---
{
"from" : {"stream": "stream1", "key": "k1"},
"joins" :
  [
    {"type" : "left",  "stream": "s2", "key":"k2", "with": "s1"},
    {"type" : "left",  "stream": "s3", "key":"k3", "with": "s1"},
    {"type" : "inner", "stream": "s4", "key":"k4", "with": "s2"}
  ],
  "outputKeys" : [ "k1", "k2" ],
  "window" : {"windowLength" : {"class":".Window$Count", "count":100}, "slidingInterval":{"class":".Window$Count", "count":100}, "tsField":null, "lagMs":0},
  "outputStream" : "joinedStream1"
}
 */


public class JoinBoltFluxComponent extends AbstractFluxComponent {


    @Override
    protected void generateComponent()  {
        String boltId        = "joinBolt_" + UUID_FOR_COMPONENTS;
        String boltClassName = "com.hortonworks.streamline.streams.runtime.storm.bolt.query.WindowedQueryBolt";

        Map<String, Object> fromSetting = ((Map<String, Object>) conf.get("from"));
        String firstStream = fromSetting.get("stream").toString();
        String firstStreamKey = fromSetting.get("key").toString();

        List boltConstructorArgs = new ArrayList();
        boltConstructorArgs.add("STREAM");
        boltConstructorArgs.add(firstStream);
        boltConstructorArgs.add(firstStreamKey);


        String[] configMethodNames = getConfiguredMethodNames(conf);
        Object[] configValues = getConfiguredMethodArgs(conf);

        List configMethods = getConfigMethodsYaml(configMethodNames, configValues);

        component = createComponent(boltId, boltClassName, null, boltConstructorArgs, configMethods);
        addParallelismToComponent();

    }

    private static String[] getConfiguredMethodNames(Map<String, Object> conf) {
        ArrayList<String> result = new ArrayList<>(conf.size());

        Object val;
        if ( (val = conf.get("joins")) != null  ) {
            for (Object joinItem : ((List<Object>) val)) {
                Map<String, Object> ji = ((Map<String, Object>) joinItem);
                String joinType = ji.get("type").toString();
                if( joinType.compareToIgnoreCase("inner")==0 )
                    result.add("join");
                else if( joinType.compareToIgnoreCase("left")==0 )
                    result.add("leftJoin");
                else
                    throw new IllegalArgumentException("Unsupported Join type: " + joinType);
            }
        }
        if( conf.containsKey("outputKeys") ) {
            result.add("selectStreamLine");
        } else {
            throw new IllegalArgumentException("'outputKeys' config is required and cannot be null");
        }

        if( conf.containsKey("window") ) {
            result.add("withWindowConfig");
        } else {
            throw new IllegalArgumentException("'window' config is required and cannot be null");
        }

        if( conf.containsKey("outputStream")) {
            result.add("withOutputStream");
        } else {
            throw new IllegalArgumentException("'outputStream' is required and cannot be null");
        }

        return result.toArray(new String[]{});
    }


    private Object[] getConfiguredMethodArgs(Map<String, Object> conf) {
        ArrayList<Object[]> result = new ArrayList<>(conf.size());

        // joins
        Object val;
        if( (val = conf.get("joins")) != null  ) {
            for (Object joinInfo : ((List<Object>) val)) {
                Map<String, Object> ji = ((Map<String, Object>) joinInfo);
                result.add( getJoinArgs(ji) );
            }
        }

        // select()
        ArrayList<String> outputKeys = (ArrayList<String>) conf.get("outputKeys");
        String outputKeysStr = String.join(",", outputKeys);
        result.add(new String[]{outputKeysStr});

        // window config
        String windowID = addWindowToComponents((Map<String, Object>) conf.get("window"));
        result.add( new Object[]{ getRefYaml(windowID) } );

        // output stream name
        String outputStreamName = conf.get("outputStream").toString();
        result.add( new String[]{outputStreamName} );

        return result.toArray(new Object[]{});
    }

    private String[] getJoinArgs(Map<String, Object> ji) {
        return new String[]{ji.get("stream").toString(), ji.get("key").toString(), ji.get("with").toString()};
    }

    // Creates component for the window and add it to the components list
    // returns the window ID
    private String addWindowToComponents(Map<String,Object> windowMap) {
        String windowId = "window_" + UUID_FOR_COMPONENTS;
        String windowClassName = "com.hortonworks.streamline.streams.layout.component.rule.expression.Window";
        List constructorArgs = new ArrayList();
        try {
            String windowJson = new ObjectMapper().writeValueAsString( windowMap );
            constructorArgs.add(windowJson);
            this.addToComponents(this.createComponent(windowId, windowClassName, null, constructorArgs, null));
            return windowId;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to crate json for window definition", e);
        }
    }

}
