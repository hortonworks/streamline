package com.hortonworks.streamline.streams.common.event.correlation;

import com.google.common.collect.Sets;
import com.hortonworks.streamline.streams.common.event.EventInformation;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class CorrelatedEventsGrouperTest {
    public static final Map<String, Object> TEST_FIELDS_AND_VALUES = Collections.singletonMap("key", "value");

    @Test
    public void testGroupEvents() throws Exception {
        long timestamp = System.currentTimeMillis();

        List<EventInformation> testEvents = new ArrayList<>();

        /*
         <SOURCE1>         <AGGREGATION>            <JOIN>         <PROJECTION>      <SINK>
           e1, e2     ->   e4 (e1 & e2)      ->  e5 (e4 & e3)  ->      e6        ->

                  <SOURCE2>
                     e3                      /

           .... and more
        */
        EventInformation event1 = new EventInformation(timestamp, "SOURCE1", "default", "AGGREGATION", "1",
                Collections.emptySet(), Collections.emptySet(), TEST_FIELDS_AND_VALUES);
        testEvents.add(event1);

        EventInformation event2 = new EventInformation(timestamp, "SOURCE1", "default", "AGGREGATION", "2",
                Collections.emptySet(), Collections.emptySet(), TEST_FIELDS_AND_VALUES);
        testEvents.add(event2);

        EventInformation event3 = new EventInformation(timestamp, "SOURCE2", "default", "JOIN", "3",
                Collections.emptySet(), Collections.emptySet(), TEST_FIELDS_AND_VALUES);
        testEvents.add(event3);

        EventInformation event4 = new EventInformation(timestamp, "AGGREGATION", "default", "JOIN", "4",
                Sets.newHashSet("1", "2"), Sets.newHashSet("1", "2"), TEST_FIELDS_AND_VALUES);
        testEvents.add(event4);

        EventInformation event5 = new EventInformation(timestamp, "JOIN", "default", "PROJECTION", "5",
                Sets.newHashSet("1", "2", "3"), Sets.newHashSet("4", "3"), TEST_FIELDS_AND_VALUES);
        testEvents.add(event5);

        EventInformation event6 = new EventInformation(timestamp, "PROJECTION", "default", "SINK", "6",
                Sets.newHashSet("1", "2", "3"), Sets.newHashSet("5"), TEST_FIELDS_AND_VALUES);
        testEvents.add(event6);

        // below two events are not correlated to root event
        EventInformation event7 = new EventInformation(timestamp, "SOURCE1", "default", "AGGREGATION", "7",
                Collections.emptySet(), Collections.emptySet(), TEST_FIELDS_AND_VALUES);
        testEvents.add(event7);

        EventInformation event8 = new EventInformation(timestamp, "SOURCE2", "default", "JOIN", "8",
                Collections.emptySet(), Collections.emptySet(), TEST_FIELDS_AND_VALUES);
        testEvents.add(event8);

        CorrelatedEventsGrouper eventsGrouper = new CorrelatedEventsGrouper(testEvents);
        GroupedCorrelationEvents groupedEvents = eventsGrouper.groupByComponent(event3.getEventId());

        Map<String, EventInformation> allEvents = groupedEvents.getAllEvents();
        // only 1 ~ 6
        Assert.assertEquals(6, allEvents.size());
        Assert.assertFalse(allEvents.containsKey("7"));
        Assert.assertFalse(allEvents.containsKey("8"));

        Map<String, GroupedCorrelationEvents.SortedComponentGroupedEvents> componentGroupedEvents =
                groupedEvents.getComponentGroupedEvents();
        GroupedCorrelationEvents.SortedComponentGroupedEvents source1 = componentGroupedEvents.get("SOURCE1");
        Assert.assertEquals("SOURCE1", source1.getComponentName());
        Assert.assertFalse(source1.isContainingSelectedEvent());
        Assert.assertTrue(source1.getInputEventIds().isEmpty());
        Assert.assertEquals(Sets.newHashSet("1", "2"), new HashSet<>(source1.getOutputEventIds()));

        GroupedCorrelationEvents.SortedComponentGroupedEvents source2 = componentGroupedEvents.get("SOURCE2");
        Assert.assertEquals("SOURCE2", source2.getComponentName());
        Assert.assertTrue(source2.isContainingSelectedEvent());
        Assert.assertTrue(source2.getInputEventIds().isEmpty());
        Assert.assertEquals(Sets.newHashSet("3"), new HashSet<>(source2.getOutputEventIds()));

        GroupedCorrelationEvents.SortedComponentGroupedEvents aggregation = componentGroupedEvents.get("AGGREGATION");
        Assert.assertEquals("AGGREGATION", aggregation.getComponentName());
        Assert.assertFalse(aggregation.isContainingSelectedEvent());
        Assert.assertEquals(Sets.newHashSet("1", "2"), new HashSet<>(aggregation.getInputEventIds()));
        Assert.assertEquals(Sets.newHashSet("4"), new HashSet<>(aggregation.getOutputEventIds()));

        GroupedCorrelationEvents.SortedComponentGroupedEvents join = componentGroupedEvents.get("JOIN");
        Assert.assertEquals("JOIN", join.getComponentName());
        Assert.assertFalse(join.isContainingSelectedEvent());
        Assert.assertEquals(Sets.newHashSet("4", "3"), new HashSet<>(join.getInputEventIds()));
        Assert.assertEquals(Sets.newHashSet("5"), new HashSet<>(join.getOutputEventIds()));

        GroupedCorrelationEvents.SortedComponentGroupedEvents projection = componentGroupedEvents.get("PROJECTION");
        Assert.assertEquals("PROJECTION", projection.getComponentName());
        Assert.assertFalse(projection.isContainingSelectedEvent());
        Assert.assertEquals(Sets.newHashSet("5"), new HashSet<>(projection.getInputEventIds()));
        Assert.assertEquals(Sets.newHashSet("6"), new HashSet<>(projection.getOutputEventIds()));

        GroupedCorrelationEvents.SortedComponentGroupedEvents sink = componentGroupedEvents.get("SINK");
        Assert.assertEquals("SINK", sink.getComponentName());
        Assert.assertFalse(sink.isContainingSelectedEvent());
        Assert.assertEquals(Sets.newHashSet("6"), new HashSet<>(sink.getInputEventIds()));
        Assert.assertTrue(sink.getOutputEventIds().isEmpty());

    }
}