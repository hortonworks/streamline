package com.hortonworks.streamline.streams.common.event.correlation;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hortonworks.streamline.streams.common.event.EventInformation;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

public class GroupedCorrelationEventsTest {

    public static final Map<String, Object> TEST_FIELDS_AND_VALUES = Collections.singletonMap("key", "value");

    @Test
    public void testBuildGroupedCorrelationEvents() throws IOException {
        long timestamp = System.currentTimeMillis();

        Map<String, EventInformation> testEventsMap = new HashMap<>();

        /*
         <SOURCE1>                  <JOIN>               <AGGREGATION>      <SINK>
             e1         -> e4 (e1 & e2), e5 (e1 & e3) ->  e6 (e4 & e5)  ->

         <SOURCE2>
           e2, e3       /
        */
        EventInformation event1 = new EventInformation(timestamp, "SOURCE1", "default", "JOIN", "1",
                Collections.emptySet(), Collections.emptySet(), TEST_FIELDS_AND_VALUES);
        testEventsMap.put(event1.getEventId(), event1);

        EventInformation event2 = new EventInformation(timestamp, "SOURCE2", "default", "JOIN", "2",
                Collections.emptySet(), Collections.emptySet(), TEST_FIELDS_AND_VALUES);
        testEventsMap.put(event2.getEventId(), event2);

        EventInformation event3 = new EventInformation(timestamp, "SOURCE2", "default", "JOIN", "3",
                Collections.emptySet(), Collections.emptySet(), TEST_FIELDS_AND_VALUES);
        testEventsMap.put(event3.getEventId(), event3);

        EventInformation event4 = new EventInformation(timestamp, "JOIN", "default", "AGGREGATION", "4",
                Sets.newHashSet("1", "2"), Sets.newHashSet("1", "2"), TEST_FIELDS_AND_VALUES);
        testEventsMap.put(event4.getEventId(), event4);

        EventInformation event5 = new EventInformation(timestamp, "JOIN", "default", "AGGREGATION", "5",
                Sets.newHashSet("1", "3"), Sets.newHashSet("1", "3"), TEST_FIELDS_AND_VALUES);
        testEventsMap.put(event5.getEventId(), event5);

        EventInformation event6 = new EventInformation(timestamp, "AGGREGATION", "default", "SINK", "6",
                Sets.newHashSet("1", "2", "3"), Sets.newHashSet("5", "6"), TEST_FIELDS_AND_VALUES);
        testEventsMap.put(event6.getEventId(), event6);

        // event 1 is being selected
        GroupedCorrelationEvents sut = new GroupedCorrelationEvents(testEventsMap, "1");
        Map<String, EventInformation> allEvents = sut.getAllEvents();
        Assert.assertEquals(testEventsMap, allEvents);

        Map<String, GroupedCorrelationEvents.SortedComponentGroupedEvents> componentGroupedEvents =
                sut.getComponentGroupedEvents();
        GroupedCorrelationEvents.SortedComponentGroupedEvents source1 = componentGroupedEvents.get("SOURCE1");
        Assert.assertEquals("SOURCE1", source1.getComponentName());
        Assert.assertTrue(source1.isContainingSelectedEvent());
        Assert.assertTrue(source1.getInputEventIds().isEmpty());
        // guarantee: selected event placed first
        Assert.assertEquals(Lists.newArrayList("1"), source1.getOutputEventIds());

        GroupedCorrelationEvents.SortedComponentGroupedEvents source2 = componentGroupedEvents.get("SOURCE2");
        Assert.assertEquals("SOURCE2", source2.getComponentName());
        Assert.assertFalse(source2.isContainingSelectedEvent());
        Assert.assertTrue(source2.getInputEventIds().isEmpty());
        // no sequence guarantee
        Assert.assertEquals(Sets.newHashSet("2", "3"), new HashSet<>(source2.getOutputEventIds()));

        GroupedCorrelationEvents.SortedComponentGroupedEvents join = componentGroupedEvents.get("JOIN");
        Assert.assertEquals("JOIN", join.getComponentName());
        Assert.assertFalse(join.isContainingSelectedEvent());
        // no sequence guarantee
        Assert.assertEquals(Sets.newHashSet("1", "2", "3"), new HashSet<>(join.getInputEventIds()));
        // no sequence guarantee
        Assert.assertEquals(Sets.newHashSet("4", "5"), new HashSet<>(join.getOutputEventIds()));

        GroupedCorrelationEvents.SortedComponentGroupedEvents aggregation = componentGroupedEvents.get("AGGREGATION");
        Assert.assertEquals("AGGREGATION", aggregation.getComponentName());
        Assert.assertFalse(aggregation.isContainingSelectedEvent());
        // no sequence guarantee
        Assert.assertEquals(Sets.newHashSet("4", "5"), new HashSet<>(aggregation.getInputEventIds()));
        // no sequence guarantee
        Assert.assertEquals(Sets.newHashSet("6"), new HashSet<>(aggregation.getOutputEventIds()));

        GroupedCorrelationEvents.SortedComponentGroupedEvents sink = componentGroupedEvents.get("SINK");
        Assert.assertEquals("SINK", sink.getComponentName());
        Assert.assertFalse(sink.isContainingSelectedEvent());
        // no sequence guarantee
        Assert.assertEquals(Sets.newHashSet("6"), new HashSet<>(sink.getInputEventIds()));
        Assert.assertTrue(sink.getOutputEventIds().isEmpty());

        // event 2 is being selected
        sut = new GroupedCorrelationEvents(testEventsMap, "2");
        componentGroupedEvents = sut.getComponentGroupedEvents();

        source1 = componentGroupedEvents.get("SOURCE1");
        Assert.assertEquals("SOURCE1", source1.getComponentName());
        Assert.assertFalse(source1.isContainingSelectedEvent());
        Assert.assertTrue(source1.getInputEventIds().isEmpty());
        // no sequence guarantee
        Assert.assertEquals(Sets.newHashSet("1"), new HashSet<>(source1.getOutputEventIds()));

        source2 = componentGroupedEvents.get("SOURCE2");
        Assert.assertEquals("SOURCE2", source2.getComponentName());
        Assert.assertTrue(source2.isContainingSelectedEvent());
        Assert.assertTrue(source2.getInputEventIds().isEmpty());
        // guarantee: selected event placed first
        Assert.assertEquals(Lists.newArrayList("2", "3"), source2.getOutputEventIds());

        // other components are same

        // event 3 is being selected
        sut = new GroupedCorrelationEvents(testEventsMap, "3");
        componentGroupedEvents = sut.getComponentGroupedEvents();

        source2 = componentGroupedEvents.get("SOURCE2");
        Assert.assertEquals("SOURCE2", source2.getComponentName());
        Assert.assertTrue(source2.isContainingSelectedEvent());
        Assert.assertTrue(source2.getInputEventIds().isEmpty());
        // guarantee: selected event placed first
        Assert.assertEquals(Lists.newArrayList("3", "2"), source2.getOutputEventIds());

        // other components are same

    }


    @Test
    public void testGroupedCorrelationPlacingEventsMatchedRootIdFirst() throws IOException {
        long timestamp = System.currentTimeMillis();

        Map<String, EventInformation> testEventsMap = new HashMap<>();

        /*
         <SOURCE1>         <PROJECTION>        <AGGREGATION>          <JOIN>           <SINK>
           e1, e2     ->  e4 (e1), e5 (e2) ->  e6 (e4 & e5)  ->  e7 (e6 & e3)  ->
         <SOURCE2>
             e3                                              /
        */
        EventInformation event1 = new EventInformation(timestamp, "SOURCE1", "default", "PROJECTION", "1",
                Collections.emptySet(), Collections.emptySet(), TEST_FIELDS_AND_VALUES);
        testEventsMap.put(event1.getEventId(), event1);

        EventInformation event2 = new EventInformation(timestamp, "SOURCE1", "default", "PROJECTION", "2",
                Collections.emptySet(), Collections.emptySet(), TEST_FIELDS_AND_VALUES);
        testEventsMap.put(event2.getEventId(), event2);

        EventInformation event3 = new EventInformation(timestamp, "SOURCE2", "default", "JOIN", "3",
                Collections.emptySet(), Collections.emptySet(), TEST_FIELDS_AND_VALUES);
        testEventsMap.put(event3.getEventId(), event3);

        EventInformation event4 = new EventInformation(timestamp, "PROJECTION", "default", "AGGREGATION", "4",
                Sets.newHashSet("1"), Sets.newHashSet("1"), TEST_FIELDS_AND_VALUES);
        testEventsMap.put(event4.getEventId(), event4);

        EventInformation event5 = new EventInformation(timestamp, "PROJECTION", "default", "AGGREGATION", "5",
                Sets.newHashSet("2"), Sets.newHashSet("2"), TEST_FIELDS_AND_VALUES);
        testEventsMap.put(event5.getEventId(), event5);

        EventInformation event6 = new EventInformation(timestamp, "AGGREGATION", "default", "JOIN", "6",
                Sets.newHashSet("1", "2"), Sets.newHashSet("4", "5"), TEST_FIELDS_AND_VALUES);
        testEventsMap.put(event6.getEventId(), event6);

        EventInformation event7 = new EventInformation(timestamp, "JOIN", "default", "SINK", "7",
                Sets.newHashSet("1", "2", "3"), Sets.newHashSet("6", "3"), TEST_FIELDS_AND_VALUES);
        testEventsMap.put(event7.getEventId(), event7);

        // event 1 is being selected
        GroupedCorrelationEvents sut = new GroupedCorrelationEvents(testEventsMap, "1");
        Map<String, EventInformation> allEvents = sut.getAllEvents();
        Assert.assertEquals(testEventsMap, allEvents);

        Map<String, GroupedCorrelationEvents.SortedComponentGroupedEvents> componentGroupedEvents =
                sut.getComponentGroupedEvents();
        GroupedCorrelationEvents.SortedComponentGroupedEvents source1 = componentGroupedEvents.get("SOURCE1");
        Assert.assertEquals("SOURCE1", source1.getComponentName());
        Assert.assertTrue(source1.isContainingSelectedEvent());
        Assert.assertTrue(source1.getInputEventIds().isEmpty());
        // guarantee: selected event placed first
        Assert.assertEquals(Lists.newArrayList("1", "2"), source1.getOutputEventIds());

        GroupedCorrelationEvents.SortedComponentGroupedEvents source2 = componentGroupedEvents.get("SOURCE2");
        Assert.assertEquals("SOURCE2", source2.getComponentName());
        Assert.assertFalse(source2.isContainingSelectedEvent());
        Assert.assertTrue(source2.getInputEventIds().isEmpty());
        // no sequence guarantee
        Assert.assertEquals(Sets.newHashSet("3"), new HashSet<>(source2.getOutputEventIds()));

        GroupedCorrelationEvents.SortedComponentGroupedEvents projection = componentGroupedEvents.get("PROJECTION");
        Assert.assertEquals("PROJECTION", projection.getComponentName());
        Assert.assertFalse(projection.isContainingSelectedEvent());
        // guarantee: selected event placed first
        Assert.assertEquals(Lists.newArrayList("1", "2"), projection.getInputEventIds());
        // guarantee: events which contains selected event as one of root ids placed earlier
        Assert.assertEquals(Lists.newArrayList("4", "5"), projection.getOutputEventIds());

        GroupedCorrelationEvents.SortedComponentGroupedEvents aggregation = componentGroupedEvents.get("AGGREGATION");
        Assert.assertEquals("AGGREGATION", aggregation.getComponentName());
        Assert.assertFalse(aggregation.isContainingSelectedEvent());
        // guarantee: events which contains selected event as one of root ids placed earlier
        Assert.assertEquals(Lists.newArrayList("4", "5"), aggregation.getInputEventIds());
        // no sequence guarantee
        Assert.assertEquals(Lists.newArrayList("6"), aggregation.getOutputEventIds());

        GroupedCorrelationEvents.SortedComponentGroupedEvents join = componentGroupedEvents.get("JOIN");
        Assert.assertEquals("JOIN", join.getComponentName());
        Assert.assertFalse(join.isContainingSelectedEvent());
        // guarantee: events which contains selected event as one of root ids placed earlier
        Assert.assertEquals(Lists.newArrayList("6", "3"), join.getInputEventIds());
        // no sequence guarantee
        Assert.assertEquals(Sets.newHashSet("7"), new HashSet<>(join.getOutputEventIds()));

        GroupedCorrelationEvents.SortedComponentGroupedEvents sink = componentGroupedEvents.get("SINK");
        Assert.assertEquals("SINK", sink.getComponentName());
        Assert.assertFalse(sink.isContainingSelectedEvent());
        // no sequence guarantee
        Assert.assertEquals(Sets.newHashSet("7"), new HashSet<>(sink.getInputEventIds()));
        Assert.assertTrue(sink.getOutputEventIds().isEmpty());

        // event 2 is being selected
        sut = new GroupedCorrelationEvents(testEventsMap, "2");
        componentGroupedEvents = sut.getComponentGroupedEvents();

        source1 = componentGroupedEvents.get("SOURCE1");
        Assert.assertEquals("SOURCE1", source1.getComponentName());
        Assert.assertTrue(source1.isContainingSelectedEvent());
        Assert.assertTrue(source1.getInputEventIds().isEmpty());
        // guarantee: selected event placed first
        Assert.assertEquals(Lists.newArrayList("2", "1"), source1.getOutputEventIds());

        projection = componentGroupedEvents.get("PROJECTION");
        Assert.assertEquals("PROJECTION", projection.getComponentName());
        Assert.assertFalse(projection.isContainingSelectedEvent());
        // guarantee: selected event placed first
        Assert.assertEquals(Lists.newArrayList("2", "1"), projection.getInputEventIds());
        // guarantee: events which contains selected event as one of root ids placed earlier
        Assert.assertEquals(Lists.newArrayList("5", "4"), projection.getOutputEventIds());

        aggregation = componentGroupedEvents.get("AGGREGATION");
        Assert.assertEquals("AGGREGATION", aggregation.getComponentName());
        Assert.assertFalse(aggregation.isContainingSelectedEvent());
        // guarantee: events which contains selected event as one of root ids placed earlier
        Assert.assertEquals(Lists.newArrayList("5", "4"), aggregation.getInputEventIds());
        // no sequence guarantee
        Assert.assertEquals(Lists.newArrayList("6"), aggregation.getOutputEventIds());

        // other components are same

        // event 3 is being selected
        sut = new GroupedCorrelationEvents(testEventsMap, "3");
        componentGroupedEvents = sut.getComponentGroupedEvents();

        source2 = componentGroupedEvents.get("SOURCE2");
        Assert.assertEquals("SOURCE2", source2.getComponentName());
        Assert.assertTrue(source2.isContainingSelectedEvent());
        Assert.assertTrue(source2.getInputEventIds().isEmpty());
        // guarantee: selected event placed first
        Assert.assertEquals(Lists.newArrayList("3"), source2.getOutputEventIds());

        // other components are same
    }


}