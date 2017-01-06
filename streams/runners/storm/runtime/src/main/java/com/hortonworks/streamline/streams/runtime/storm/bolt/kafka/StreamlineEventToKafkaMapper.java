package com.hortonworks.streamline.streams.runtime.storm.bolt.kafka;


import org.apache.storm.kafka.bolt.mapper.TupleToKafkaMapper;
import org.apache.storm.tuple.Tuple;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.runtime.storm.StreamlineRuntimeUtil;

public class StreamlineEventToKafkaMapper implements TupleToKafkaMapper {
    private final String keyName;

    public StreamlineEventToKafkaMapper (String keyName) {
        this.keyName = keyName;
    }

    @Override
    public Object getKeyFromTuple (Tuple tuple) {
        StreamlineEvent streamlineEvent = (StreamlineEvent) tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
        return StreamlineRuntimeUtil.getFieldValue(streamlineEvent, keyName);
    }

    @Override
    public Object getMessageFromTuple (Tuple tuple) {
        return tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
    }
}
