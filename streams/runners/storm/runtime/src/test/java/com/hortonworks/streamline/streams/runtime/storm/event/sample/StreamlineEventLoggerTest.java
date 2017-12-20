package com.hortonworks.streamline.streams.runtime.storm.event.sample;

import com.google.common.collect.ImmutableMap;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import com.hortonworks.streamline.streams.common.event.correlation.EventCorrelationInjector;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.storm.metric.IEventLogger;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StreamlineEventLoggerTest {
    private FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS");
    private StreamlineEventLogger sut = new StreamlineEventLogger();

    @Test
    public void buildStreamlineEventLogMessage() {
        long timestamp = 1513300663123L;
        String timestampStr = dateFormat.format(timestamp);
        String stormComponentName = "1-Component";
        String streamlineComponent = "Component";
        int taskId = 2;
        Object messageId = "dummy";

        // test root event
        StreamlineEvent event = buildTestEvent();

        List<Object> values = Collections.singletonList(event);

        String expectMessage = String.format("%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s", timestampStr,
                StreamlineEventLogger.DELIMITER, StreamlineEventLogger.MARKER_FOR_STREAMLINE_EVENT,
                StreamlineEventLogger.DELIMITER, streamlineComponent, StreamlineEventLogger.DELIMITER,
                event.getId(), StreamlineEventLogger.DELIMITER, "[]", StreamlineEventLogger.DELIMITER,
                "[]", StreamlineEventLogger.DELIMITER, ImmutableMap.copyOf(event), StreamlineEventLogger.DELIMITER,
                event.getHeader(), StreamlineEventLogger.DELIMITER, event.getAuxiliaryFieldsAndValues());

        IEventLogger.EventInfo eventInfo = new IEventLogger.EventInfo(timestamp, stormComponentName, taskId,
                messageId, values);

        String actualMessage = sut.buildLogMessage(eventInfo);
        Assert.assertEquals(expectMessage, actualMessage);

        // test event with event correlation information
        EventCorrelationInjector eventCorrelationInjector = new EventCorrelationInjector();

        StreamlineEvent event2 = StreamlineEventImpl.builder().from(event).build();
        StreamlineEvent event3 = StreamlineEventImpl.builder().from(event).build();

        event2 = eventCorrelationInjector.injectCorrelationInformation(event2, Collections.singletonList(event3),
                streamlineComponent);

        event = eventCorrelationInjector.injectCorrelationInformation(event, Collections.singletonList(event2),
                streamlineComponent);

        values = Collections.singletonList(event);

        expectMessage = String.format("%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s%s", timestampStr,
                StreamlineEventLogger.DELIMITER, StreamlineEventLogger.MARKER_FOR_STREAMLINE_EVENT,
                StreamlineEventLogger.DELIMITER, streamlineComponent, StreamlineEventLogger.DELIMITER,
                event.getId(), StreamlineEventLogger.DELIMITER, "[" + event3.getId() + "]",
                StreamlineEventLogger.DELIMITER, "[" + event2.getId() + "]",
                StreamlineEventLogger.DELIMITER, ImmutableMap.copyOf(event), StreamlineEventLogger.DELIMITER,
                event.getHeader(), StreamlineEventLogger.DELIMITER, event.getAuxiliaryFieldsAndValues());

        eventInfo = new IEventLogger.EventInfo(timestamp, stormComponentName, taskId, messageId, values);

        actualMessage = sut.buildLogMessage(eventInfo);
        Assert.assertEquals(expectMessage, actualMessage);
    }

    @Test
    public void buildOtherEventLogMessage() {
        long timestamp = 1513300663123L;
        String timestampStr = dateFormat.format(timestamp);
        String stormComponentName = "1-Component";
        int taskId = 2;
        Object messageId = "dummy";

        List<Object> values = new ArrayList<>();
        values.add("hello");
        values.add("world");
        values.add(12345);

        // Date, Marker, Component Name (Storm), task ID, Message ID, Values
        String expectMessage = String.format("%s,%s,%s,%s,%s,%s", timestampStr,
                StreamlineEventLogger.MARKER_FOR_OTHER_EVENT, stormComponentName, taskId, messageId, values);

        IEventLogger.EventInfo eventInfo = new IEventLogger.EventInfo(timestamp, stormComponentName, taskId,
                messageId, values);

        String actualMessage = sut.buildLogMessage(eventInfo);
        Assert.assertEquals(expectMessage, actualMessage);
    }

    private StreamlineEvent buildTestEvent() {
        Map<String, Object> kv = new HashMap<>();
        kv.put("key1", "value1");
        kv.put("key2", 1);

        Map<String, Object> header = new HashMap<>();
        kv.put("header1", "value1");
        kv.put("header2", 2);

        Map<String, Object> aux = new HashMap<>();
        aux.put("aux1", "value1");
        aux.put("aux2", 2);

        // root event
        return StreamlineEventImpl.builder()
                .putAll(kv).header(header).auxiliaryFieldsAndValues(aux).build();
    }
}