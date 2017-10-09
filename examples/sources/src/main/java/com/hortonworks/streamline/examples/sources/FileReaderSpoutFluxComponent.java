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
package com.hortonworks.streamline.examples.sources;

import com.hortonworks.streamline.streams.layout.TopologyLayoutConstants;
import com.hortonworks.streamline.streams.layout.component.StreamlineSource;
import com.hortonworks.streamline.streams.layout.storm.AbstractFluxComponent;
import com.hortonworks.streamline.streams.layout.storm.StormTopologyLayoutConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class FileReaderSpoutFluxComponent extends AbstractFluxComponent {

    private static final Logger LOG = LoggerFactory.getLogger(FileReaderSpoutFluxComponent.class);

    @Override
    protected void generateComponent() {
        StreamlineSource streamlineSource = (StreamlineSource) conf.get(StormTopologyLayoutConstants.STREAMLINE_COMPONENT_CONF_KEY);
        // add the output stream to conf so that the kafka spout declares output stream properly
        if (streamlineSource != null && streamlineSource.getOutputStreams().size() == 1) {
            conf.put(TopologyLayoutConstants.JSON_KEY_OUTPUT_STREAM_ID,
                    streamlineSource.getOutputStreams().iterator().next().getId());
        } else {
            String msg = "FileReaderSpout source component [" + streamlineSource + "] should define exactly one output stream for Storm";
            LOG.error(msg, streamlineSource);
            throw new IllegalArgumentException(msg);
        }
        String spoutId = "fileReaderSpout" + UUID_FOR_COMPONENTS;
        String spoutClassName = "com.hortonworks.streamline.examples.sources.FileReaderSpout";
        String[] contructorArgNames = {"path"};
        List<Object> configMethods = new ArrayList<>();
        String[] configMethodNames = { "withOutputStream", "withDelimiter"};
        String[] configKeys = { TopologyLayoutConstants.JSON_KEY_OUTPUT_STREAM_ID, "delimiter"};
        configMethods.addAll(getConfigMethodsYaml(configMethodNames, configKeys));
        component = createComponent(spoutId, spoutClassName, null, getConstructorArgsYaml(contructorArgNames), configMethods);
        addParallelismToComponent();
    }
}
