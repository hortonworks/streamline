/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.hortonworks.streamline.streams.layout.storm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;


/* ---- Sample Json of whats expected from UI  ---
{
"from" : {"stream": "orders"},
"join" : {"type" : "inner", "stream" : "adImpressions", "count" : 10, "dropDuplicates" : false},  # here we can have:  count/seconds/minutes/milliseconds
"equal" :
  [
    { "firstKey" : "userID",    "secondKey" : "userid"},
    { "firstKey" : "productID", "secondKey" : "productID"}
  ],
  "outputKeys" : [ "userID", "orders:productID" ,"orderId", "impressionId" ],
  "outputStream" : "joinedStream1"
}
 */

public class RealtimeJoinBoltFluxComponent extends AbstractFluxComponent {

    @Override
    protected void generateComponent()  {
        String boltId        = "rt_joinBolt_" + UUID_FOR_COMPONENTS;
        String boltClassName = "com.hortonworks.streamline.streams.runtime.storm.bolt.query.RealtimeJoinBolt";

        List<String> boltConstructorArgs = new ArrayList<>();

        String[] configMethodNames = getConfiguredMethodNames(conf);
        Object[] configValues = getConfiguredMethodArgs(conf);

        List<Map<String, Object>> configMethods = getConfigMethodsYaml(configMethodNames, configValues);

        component = createComponent(boltId, boltClassName, null, boltConstructorArgs, configMethods);
        addParallelismToComponent();

    }

    private static String[] getConfiguredMethodNames(Map<String, Object> conf) {
        ArrayList<String> result = new ArrayList<>(conf.size());

        if ( conf.containsKey("from") ) {
            result.add("dataStream");
        } else {
            throw new IllegalArgumentException("'from' parameter is required and cannot be null");
        }

        Map<String, Object> joinConfig = (Map<String, Object>) conf.get("join");
        if ( joinConfig!=null ) {
            String joinType = joinConfig.get("type").toString();
            if (joinType.equalsIgnoreCase("inner")) {
                result.add("innerJoin");
            } else if (joinType.equalsIgnoreCase("left")) {
                result.add("leftJoin");
            } else if (joinType.equalsIgnoreCase("right")) {
                result.add("rightJoin");
            } else if (joinType.equalsIgnoreCase("outer")) {
                result.add("outerJoin");
            } else {
                throw new IllegalArgumentException("Allowed join types : 'inner'/'left'/'right'/'outer'");
            }
        } else {
            throw new IllegalArgumentException("'join' configuration missing");
        }

        Object val;
        if ( (val = conf.get("equal")) != null  ) {
            for (Object equalMethod : ((List<Object>) val)) {
                result.add("streamlineEqual");
            }
        }

        if ( conf.containsKey("outputKeys") ) {
            result.add("streamlineSelect");
        } else {
            throw new IllegalArgumentException("'outputKeys' parameter is required and cannot be null");
        }

        if ( conf.containsKey("outputStream")) {
            result.add("withOutputStream");
        } else {
            throw new IllegalArgumentException("'outputStream' parameter is required and cannot be null");
        }

        return result.toArray(new String[]{});
    }


    private Object[] getConfiguredMethodArgs(Map<String, Object> conf) {
        ArrayList<Object[]> result = new ArrayList<>(conf.size());

        // dataStream()
        String fromStream = ((Map<String,Object>)conf.get("from")).get("stream").toString();
        result.add( new String[]{fromStream} );


        // *Join()
        Map<String, Object> joinConf = (Map<String, Object>) conf.get("join");
        String stream = joinConf.get("stream").toString();
        String countOrDurationId =  addCountOrDuration(joinConf);
        Boolean dropDuplicates = (Boolean) joinConf.get("dropDuplicates");
        result.add(new Object[]{stream, getRefYaml(countOrDurationId), dropDuplicates });


        // streamlineEqual()
        Object val;
        if( (val = conf.get("equal")) != null  ) {
            for (Object joinKeys : ((List<Object>) val)) {
                Map<String, Object> ji = ((Map<String, Object>) joinKeys);
                String firstKey = ji.get("firstKey").toString();
                String secondKey = ji.get("secondKey").toString();
                result.add( new String[]{firstKey, secondKey} );
            }
        }

        // streamlineSelect()
        ArrayList<String> outputKeys = (ArrayList<String>) conf.get("outputKeys");
        String outputKeysStr = String.join(",", outputKeys);
        result.add(new String[]{outputKeysStr});

        // withOutputStream()
        String outputStreamName = conf.get("outputStream").toString();
        result.add( new String[]{outputStreamName} );

        return result.toArray(new Object[]{});
    }

    // returns component ID
    private String addCountOrDuration(Map<String, Object> joinArgs) {
        Integer count = (Integer) joinArgs.get("count");
        if( count != null ) {
            return addCountToComponents(count);
        }
        return addDurationToComponents( joinArgs );
    }


    // Creates component for the Count object and adds it to the components list
    // returns the component ID
    private String addCountToComponents(Integer count) {
        String componentId = "duration_" + UUID_FOR_COMPONENTS;
        String className = "org.apache.storm.topology.base.BaseWindowedBolt.Count";
        List<Object> constructorArgs = new ArrayList<>();
        try {
            constructorArgs.add(count);
            this.addToComponents(this.createComponent(componentId, className, null, constructorArgs, null));
            return componentId;
        } catch (Exception e) {
            throw new RuntimeException("Unable to crate json for window definition", e);
        }
    }

    // Creates component for the Duration object and adds it to the components list
    // returns the component ID
    private String addDurationToComponents(Map<String, Object> joinArgs) {
        String componentId = "duration_" + UUID_FOR_COMPONENTS;
        String className = "org.apache.storm.topology.base.BaseWindowedBolt.Duration";

        // Find dthe TimeUnit.enums key in joinArgs .... secs/mins/millis/days/etc
        String units = Stream.of(TimeUnit.values()).map(TimeUnit::name).filter(
                k -> joinArgs.containsKey(k.toLowerCase())
        ).findFirst().get();
        Integer duration = (Integer) joinArgs.get(units.toLowerCase());

        List<String> constructorArgs = new ArrayList<>();
        String json = "[" + duration + ", " + TimeUnit.valueOf(units) + "]";
        constructorArgs.add(json);
        this.addToComponents(this.createComponent(componentId, className, null, constructorArgs, null));
        return componentId;
    }


}
