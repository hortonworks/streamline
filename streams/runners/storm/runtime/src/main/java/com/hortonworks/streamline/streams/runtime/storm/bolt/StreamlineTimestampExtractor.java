package com.hortonworks.streamline.streams.runtime.storm.bolt;

import com.hortonworks.streamline.streams.StreamlineEvent;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.windowing.TimestampExtractor;

/**
 * Extract timestamp value from streamline event.
 */
public class StreamlineTimestampExtractor implements TimestampExtractor {
    private final String fieldName;

    public StreamlineTimestampExtractor(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public long extractTimestamp(Tuple tuple) {
        StreamlineEvent event = (StreamlineEvent) tuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT);
        Object ts = event.get(fieldName);
        if (ts == null || !(ts instanceof Long)) {
            throw new IllegalArgumentException("Streamline event does not contain a long value in field: " + fieldName);
        }
        return (long) ts;
    }
}
