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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.hortonworks.streamline.streams.layout.component.Stream;
import com.hortonworks.streamline.streams.layout.component.impl.MultiLangProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StreamsShellBoltFluxComponent extends  AbstractFluxComponent {

    protected MultiLangProcessor multiLangProcessor;

    public StreamsShellBoltFluxComponent() {
    }

    @Override
    protected void generateComponent() {
        multiLangProcessor = (MultiLangProcessor) conf.get(StormTopologyLayoutConstants.STREAMLINE_COMPONENT_CONF_KEY);
        String boltId = "streamsShellBolt" + UUID_FOR_COMPONENTS;
        String boltClassName = "com.hortonworks.streamline.streams.runtime.storm.bolt.StreamsShellBolt";

        String[] constructorArgNames = {"command", "processTimeout"};
        List<Object> boltConstructorArgs = getConstructorArgsYaml(constructorArgNames);

        List<String> configMethodNames = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        configMethodNames.add("withOutputStreams");
        values.add(getStreams());

        List<Map<String, Object>> configMethods = getConfigMethodsYaml(configMethodNames.toArray(new String[0]), values.toArray());

        component = createComponent(boltId, boltClassName, null, boltConstructorArgs, configMethods);

        addParallelismToComponent();
    }

    private List<String> getStreams() {
        Collection<String> streams = Collections.emptyList();
        if (multiLangProcessor != null) {
            streams = Collections2.transform(multiLangProcessor.getOutputStreams(), new Function<Stream, String>() {
                @Override
                public String apply(Stream input) {
                    return input.getId();
                }
            });
        }
        return ImmutableList.copyOf(streams);
    }
}
