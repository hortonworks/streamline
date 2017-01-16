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
