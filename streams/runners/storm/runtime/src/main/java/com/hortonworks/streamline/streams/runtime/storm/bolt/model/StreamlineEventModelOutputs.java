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
package com.hortonworks.streamline.streams.runtime.storm.bolt.model;

import com.hortonworks.streamline.common.util.Utils;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.layout.component.Stream;
import com.hortonworks.streamline.streams.layout.component.impl.model.ModelProcessor;

import org.apache.storm.pmml.model.ModelOutputs;
import org.apache.storm.tuple.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StreamlineEventModelOutputs implements ModelOutputs {
    protected static final Logger LOG = LoggerFactory.getLogger(StreamlineEventModelOutputs.class);

    private final Map<String, Fields> streamFields;

    public StreamlineEventModelOutputs(String modelProcessorJson) {
        this(Utils.createObjectFromJson(modelProcessorJson, ModelProcessor.class));
    }

    public StreamlineEventModelOutputs(ModelProcessor modelProcessor) {
        streamFields = new HashMap<>();
        for (Stream stream : modelProcessor.getOutputStreams()) {
            streamFields.put(stream.getId(), new Fields(StreamlineEvent.STREAMLINE_EVENT));
        }
    }

    @Override
    public Map<String, ? extends Fields> streamFields() {
        return streamFields;
    }

    @Override
    public Set<String> streams() {
        return streamFields.keySet();
    }
}
