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

import java.util.ArrayList;
import java.util.List;

public class EventHubSpoutFluxComponent extends AbstractFluxComponent {
    private static final String KEY_USENAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_NAMESPACE = "namespace";
    private static final String KEY_ENTITYPATH = "entityPath";
    private static final String KEY_PARTITIONCOUNT = "partitionCount";

    private static final String KEY_ZK_CONNECTION_STRING = "zkConnectionString";
    private static final String KEY_CHECKPOINT_INTERVAL_SECS = "checkpointIntervalInSeconds";
    private static final String KEY_RECEIVER_CREDITS = "receiverCredits";
    private static final String KEY_MAX_PENDING_MSGS_PER_PARTITION = "maxPendingMsgsPerPartition";
    private static final String KEY_ENQUEUE_TIME_FILTER = "enqueueTimeFilter";
    private static final String KEY_CONSUMER_GROUP_NAME = "consumerGroupName";

    @Override
    protected void generateComponent() {
        String spoutId = "eventhubSpout" + UUID_FOR_COMPONENTS;
        String spoutClassName = "org.apache.storm.eventhubs.spout.EventHubSpout";
        List<Object> constructorArgs = new ArrayList<>();
        addArg(constructorArgs, getRefYaml(addEventHubSpoutConfigComponent()));
        component = createComponent(spoutId, spoutClassName, null, constructorArgs, null);
        addParallelismToComponent();
    }

    private String addEventHubSpoutConfigComponent() {
        String componentId = "eventHubSpoutConfig" + UUID_FOR_COMPONENTS;
        String className = "org.apache.storm.eventhubs.spout.EventHubSpoutConfig";
        final List<Object> constructorArgs =
                makeConstructorArgs(
                        KEY_USENAME,
                        KEY_PASSWORD,
                        KEY_NAMESPACE,
                        KEY_ENTITYPATH,
                        KEY_PARTITIONCOUNT);
        List<Object> properties = getPropertiesYaml(new String[]{
                KEY_ZK_CONNECTION_STRING, KEY_CHECKPOINT_INTERVAL_SECS, KEY_RECEIVER_CREDITS,
                KEY_MAX_PENDING_MSGS_PER_PARTITION, KEY_ENQUEUE_TIME_FILTER, KEY_CONSUMER_GROUP_NAME
        });
        addToComponents(createComponent(componentId, className, properties, constructorArgs, null));
        return componentId;
    }

}
