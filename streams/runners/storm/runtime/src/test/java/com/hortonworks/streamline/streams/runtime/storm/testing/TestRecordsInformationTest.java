package com.hortonworks.streamline.streams.runtime.storm.testing;

import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class TestRecordsInformationTest {

    @Test
    public void testIterateRecordsWithSleepPerIteration() {
        List<Map<String, Object>> records = new ArrayList<>();
        records.add(Collections.singletonMap("A", 1));
        records.add(Collections.singletonMap("A", 2));
        records.add(Collections.singletonMap("A", 3));

        TestRecordsInformation testRecordsInformation = new TestRecordsInformation(3, 1000, records);

        // first iteration
        Optional<Map<String, Object>> recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(0), recordOptional.get());

        recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(1), recordOptional.get());

        recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(2), recordOptional.get());

        // end of first iteration
        recordOptional = testRecordsInformation.nextRecord();
        assertFalse(recordOptional.isPresent());

        // sleep 300 ms
        Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);

        // still in sleep
        recordOptional = testRecordsInformation.nextRecord();
        assertFalse(recordOptional.isPresent());

        // sleep 700 ms (total 1000 ms)
        Uninterruptibles.sleepUninterruptibly(700, TimeUnit.MILLISECONDS);

        // second iteration
        recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(0), recordOptional.get());

        recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(1), recordOptional.get());

        recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(2), recordOptional.get());

        // end of second iteration
        recordOptional = testRecordsInformation.nextRecord();
        assertFalse(recordOptional.isPresent());

        // sleep 1000 ms
        Uninterruptibles.sleepUninterruptibly(1000, TimeUnit.MILLISECONDS);

        // third iteration
        recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(0), recordOptional.get());

        recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(1), recordOptional.get());

        recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(2), recordOptional.get());

        // end of third iteration (whole iteration)
        recordOptional = testRecordsInformation.nextRecord();
        assertFalse(recordOptional.isPresent());

         // sleep 1000 ms
        Uninterruptibles.sleepUninterruptibly(1000, TimeUnit.MILLISECONDS);

        // whole iterations are completed
        recordOptional = testRecordsInformation.nextRecord();
        assertFalse(recordOptional.isPresent());

    }

    @Test
    public void testIterateRecordsWithoutSleep() {
        List<Map<String, Object>> records = new ArrayList<>();
        records.add(Collections.singletonMap("A", 1));
        records.add(Collections.singletonMap("A", 2));
        records.add(Collections.singletonMap("A", 3));

        TestRecordsInformation testRecordsInformation = new TestRecordsInformation(3, 0, records);

        // first iteration
        Optional<Map<String, Object>> recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(0), recordOptional.get());

        recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(1), recordOptional.get());

        recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(2), recordOptional.get());

        // next iteration will be started immediately
        // second iteration
        recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(0), recordOptional.get());

        recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(1), recordOptional.get());

        recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(2), recordOptional.get());

        // next iteration will be started immediately
        // third iteration
        recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(0), recordOptional.get());

        recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(1), recordOptional.get());

        recordOptional = testRecordsInformation.nextRecord();
        assertTrue(recordOptional.isPresent());
        assertEquals(records.get(2), recordOptional.get());

        // end of third iteration (whole iteration)
        recordOptional = testRecordsInformation.nextRecord();
        assertFalse(recordOptional.isPresent());

         // sleep 1000 ms
        Uninterruptibles.sleepUninterruptibly(1000, TimeUnit.MILLISECONDS);

        // whole iterations are completed
        recordOptional = testRecordsInformation.nextRecord();
        assertFalse(recordOptional.isPresent());

    }

}