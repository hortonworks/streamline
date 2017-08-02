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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;


/* ---- Sample Json of whats expected from UI  ---
{
"from" : {"stream": "orders", "seconds/minutes/hours" : 10, "unique" : false },

"joins" : [
    { "type":"inner/left/right/outer",  "stream":"adImpressions",  "count/seconds/minutes/hours":10,  "unique":false,
               "conditions" : [
                  [ "equal",  "adImpressions:userID",  "orders:userId" ],
                  [ "ignoreCase", "product", "orders:product"]
               ]
     }
  ],

"outputKeys" : [ "userID", "orders:product as product" ,"orderId", "impressionId" ],
"outputStream" : "joinedStream1"
}
 */

public class SLRealtimeJoinBoltFluxComponent extends AbstractFluxComponent {

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
            result.add("from");
        } else {
            throw new IllegalArgumentException("'from' parameter is required and cannot be null");
        }

        ArrayList<Map<String, Object>> joinConfig = (ArrayList<Map<String, Object>>) conf.get("joins");
        if ( joinConfig!=null ) {
            for (Map<String, Object> join : joinConfig) {
                String joinType = join.get("type").toString();
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
            }
        } else {
            throw new IllegalArgumentException("'join' configuration missing");
        }

        if ( conf.containsKey("outputKeys") ) {
            result.add("select");
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

        {  // from()
            Map<String, Object> fromConf = (Map<String, Object>) conf.get("from");
            String fromStream = fromConf.get("stream").toString();
            Boolean unique = (Boolean) fromConf.get("unique");

            Integer count = (Integer) fromConf.get("count");
            if( count != null ) {
                result.add(new Object[]{fromStream, count, unique});
            } else {
                String durationId = addDurationToComponents(fromConf);
                result.add(new Object[]{fromStream, getRefYaml(durationId), unique});
            }
        }
        {  // *Join()
            ArrayList<Map<String, Object>> joinConfig = (ArrayList<Map<String, Object>>) conf.get("joins");
            for (Map<String, Object> joinConf : joinConfig) {
                String stream = joinConf.get("stream").toString();
                Boolean unique = (Boolean) joinConf.get("unique");

                ArrayList<Map<String, String> > comparators = new ArrayList<>();
                for (ArrayList<String> condition : (ArrayList<ArrayList<String> >) joinConf.get("conditions")) {
                    comparators.add( getRefYaml( addComparatorToComponents(condition) ) );
                }
                Integer count = (Integer) joinConf.get("count");
                if (count!=null) {
                    result.add(new Object[]{stream, count, unique, comparators.toArray()});
                } else {
                    String durationId = addDurationToComponents(joinConf);
                    result.add(new Object[]{stream, getRefYaml(durationId), unique, comparators.toArray()});
                }
            }
        }

        // select()
        ArrayList<String> outputKeys = (ArrayList<String>) conf.get("outputKeys");
        String outputKeysStr = String.join(",", outputKeys);
        result.add(new String[]{outputKeysStr});

        // withOutputStream()
        String outputStreamName = conf.get("outputStream").toString();
        result.add( new String[]{outputStreamName} );

        return result.toArray(new Object[]{});
    }

    private String addComparatorToComponents(ArrayList<String> condition) {
        String componentId = "slcmp_" + UUID.randomUUID();


        String className = null;
        String methodName = condition.get(0).toString();
        if (methodName.equalsIgnoreCase("equal"))
             className = "com.hortonworks.streamline.streams.runtime.storm.bolt.query.SLCmp.Equal";
        else if (methodName.equalsIgnoreCase("ignoreCase"))
             className = "com.hortonworks.streamline.streams.runtime.storm.bolt.query.SLCmp.IgnoreCase";

        List<String> constructorArgs = new ArrayList<>();
        for (int i = 1; i < condition.size(); i++) {
            constructorArgs.add(condition.get(i));
        }

        this.addToComponents(this.createComponent(componentId, className, null, constructorArgs, null));
        return componentId;
    }

    // Creates component for the Duration object and adds it to the components list
    // returns the component ID
    private String addDurationToComponents(Map<String, Object> joinArgs) {
        String componentId = "duration_" + UUID.randomUUID();
        String className = "org.apache.storm.topology.base.BaseWindowedBolt.Duration";

        // Find the TimeUnit.enums key in joinArgs .... secs/mins/millis/days/etc
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
