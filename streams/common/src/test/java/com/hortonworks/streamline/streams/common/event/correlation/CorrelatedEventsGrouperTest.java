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
        EventInformation event1 = new EventInformation(timestamp, "SOURCE1", "default",
                Collections.singleton("AGGREGATION"), "1",
                Collections.emptySet(), Collections.emptySet(), TEST_FIELDS_AND_VALUES);
        testEvents.add(event1);

        EventInformation event2 = new EventInformation(timestamp, "SOURCE1", "default",
                Collections.singleton("AGGREGATION"), "2",
                Collections.emptySet(), Collections.emptySet(), TEST_FIELDS_AND_VALUES);
        testEvents.add(event2);

        EventInformation event3 = new EventInformation(timestamp, "SOURCE2", "default",
                Collections.singleton("JOIN"), "3",
                Collections.emptySet(), Collections.emptySet(), TEST_FIELDS_AND_VALUES);
        testEvents.add(event3);

        EventInformation event4 = new EventInformation(timestamp, "AGGREGATION", "default",
                Collections.singleton("JOIN"), "4",
                Sets.newHashSet("1", "2"), Sets.newHashSet("1", "2"), TEST_FIELDS_AND_VALUES);
        testEvents.add(event4);

        EventInformation event5 = new EventInformation(timestamp, "JOIN", "default",
                Collections.singleton("PROJECTION"), "5",
                Sets.newHashSet("1", "2", "3"), Sets.newHashSet("4", "3"), TEST_FIELDS_AND_VALUES);
        testEvents.add(event5);

        EventInformation event6 = new EventInformation(timestamp, "PROJECTION", "default",
                Collections.singleton("SINK"), "6",
                Sets.newHashSet("1", "2", "3"), Sets.newHashSet("5"), TEST_FIELDS_AND_VALUES);
        testEvents.add(event6);

        // below two events are not correlated to root event
        EventInformation event7 = new EventInformation(timestamp, "SOURCE1", "default",
                Collections.singleton("AGGREGATION"), "7",
                Collections.emptySet(), Collections.emptySet(), TEST_FIELDS_AND_VALUES);
        testEvents.add(event7);

        EventInformation event8 = new EventInformation(timestamp, "SOURCE2", "default",
                Collections.singleton("JOIN"), "8",
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

    @Test
    public void testGroupEventsWithSplitProcessor() throws Exception {
        long timestamp = System.currentTimeMillis();

        List<EventInformation> testEvents = new ArrayList<>();

        /*
         <SOURCE>    <SPLIT>       <PROJECTION1>        <SINK1>
           e1     ->     e2     ->      e3         ->

                                \   <PROJECTION2>
                                        e4         ->

                                \   <PROJECTION3>
                                        e5         ->
        */
        EventInformation event1 = new EventInformation(timestamp, "SOURCE", "default",
                Collections.singleton("SPLIT"), "1",
                Collections.emptySet(), Collections.emptySet(), TEST_FIELDS_AND_VALUES);
        testEvents.add(event1);

        EventInformation event2 = new EventInformation(timestamp, "SPLIT", "stream1",
                Sets.newHashSet("PROJECTION1", "PROJECTION2", "PROJECTION3"), "2",
                Sets.newHashSet("1"), Sets.newHashSet("1"), TEST_FIELDS_AND_VALUES);
        testEvents.add(event2);

        EventInformation event3 = new EventInformation(timestamp, "PROJECTION1", "default",
                Collections.singleton("SINK1"), "3",
                Sets.newHashSet("1"), Sets.newHashSet("2"), TEST_FIELDS_AND_VALUES);
        testEvents.add(event3);

        EventInformation event4 = new EventInformation(timestamp, "PROJECTION2", "default",
                Collections.singleton("SINK2"), "4",
                Sets.newHashSet("1"), Sets.newHashSet("2"), TEST_FIELDS_AND_VALUES);
        testEvents.add(event4);

        EventInformation event5 = new EventInformation(timestamp, "PROJECTION3", "default",
                Collections.singleton("SINK3"), "5",
                Sets.newHashSet("1"), Sets.newHashSet("2"), TEST_FIELDS_AND_VALUES);
        testEvents.add(event5);

        CorrelatedEventsGrouper eventsGrouper = new CorrelatedEventsGrouper(testEvents);
        GroupedCorrelationEvents groupedEvents = eventsGrouper.groupByComponent(event1.getEventId());

        Map<String, EventInformation> allEvents = groupedEvents.getAllEvents();
        Assert.assertEquals(5, allEvents.size());

        Map<String, GroupedCorrelationEvents.SortedComponentGroupedEvents> componentGroupedEvents =
                groupedEvents.getComponentGroupedEvents();
        GroupedCorrelationEvents.SortedComponentGroupedEvents source = componentGroupedEvents.get("SOURCE");
        Assert.assertEquals("SOURCE", source.getComponentName());
        Assert.assertTrue(source.isContainingSelectedEvent());
        Assert.assertTrue(source.getInputEventIds().isEmpty());
        Assert.assertEquals(Sets.newHashSet("1"), new HashSet<>(source.getOutputEventIds()));

        GroupedCorrelationEvents.SortedComponentGroupedEvents split = componentGroupedEvents.get("SPLIT");
        Assert.assertEquals("SPLIT", split.getComponentName());
        Assert.assertFalse(split.isContainingSelectedEvent());
        Assert.assertEquals(Collections.singletonList("1"), split.getInputEventIds());
        Assert.assertEquals(Collections.singletonList("2"), split.getOutputEventIds());

        GroupedCorrelationEvents.SortedComponentGroupedEvents projection1 = componentGroupedEvents.get("PROJECTION1");
        Assert.assertEquals("PROJECTION1", projection1.getComponentName());
        Assert.assertFalse(projection1.isContainingSelectedEvent());
        Assert.assertEquals(Collections.singletonList("2"), projection1.getInputEventIds());
        Assert.assertEquals(Collections.singletonList("3"), projection1.getOutputEventIds());

        GroupedCorrelationEvents.SortedComponentGroupedEvents projection2 = componentGroupedEvents.get("PROJECTION2");
        Assert.assertEquals("PROJECTION2", projection2.getComponentName());
        Assert.assertFalse(projection2.isContainingSelectedEvent());
        Assert.assertEquals(Collections.singletonList("2"), projection2.getInputEventIds());
        Assert.assertEquals(Collections.singletonList("4"), projection2.getOutputEventIds());

        GroupedCorrelationEvents.SortedComponentGroupedEvents projection3 = componentGroupedEvents.get("PROJECTION3");
        Assert.assertEquals("PROJECTION3", projection3.getComponentName());
        Assert.assertFalse(projection3.isContainingSelectedEvent());
        Assert.assertEquals(Collections.singletonList("2"), projection3.getInputEventIds());
        Assert.assertEquals(Collections.singletonList("5"), projection3.getOutputEventIds());

        GroupedCorrelationEvents.SortedComponentGroupedEvents sink1 = componentGroupedEvents.get("SINK1");
        Assert.assertEquals("SINK1", sink1.getComponentName());
        Assert.assertFalse(sink1.isContainingSelectedEvent());
        Assert.assertEquals(Collections.singletonList("3"), sink1.getInputEventIds());
        Assert.assertTrue(sink1.getOutputEventIds().isEmpty());

        GroupedCorrelationEvents.SortedComponentGroupedEvents sink2 = componentGroupedEvents.get("SINK2");
        Assert.assertEquals("SINK2", sink2.getComponentName());
        Assert.assertFalse(sink2.isContainingSelectedEvent());
        Assert.assertEquals(Collections.singletonList("4"), sink2.getInputEventIds());
        Assert.assertTrue(sink2.getOutputEventIds().isEmpty());

        GroupedCorrelationEvents.SortedComponentGroupedEvents sink3 = componentGroupedEvents.get("SINK3");
        Assert.assertEquals("SINK3", sink3.getComponentName());
        Assert.assertFalse(sink3.isContainingSelectedEvent());
        Assert.assertEquals(Collections.singletonList("5"), sink3.getInputEventIds());
        Assert.assertTrue(sink3.getOutputEventIds().isEmpty());

    }
}