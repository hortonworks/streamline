package com.hortonworks.streamline.streams.runtime.storm.grouping;

import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link FieldsGroupingAsCustomGrouping}
 */
public class FieldsGroupingAsCustomGroupingTest {
    @Test
    public void chooseTasksNegativeHashcode() throws Exception {
        FieldsGroupingAsCustomGrouping fg = new FieldsGroupingAsCustomGrouping(Collections.singletonList("A"));
        Map<String, Object> keyValues = Collections.singletonMap("A", "PUFPaY9KxDAcGqfsorJp1R");
        fg.prepare(null, null, Arrays.asList(0, 1));
        List<Integer> res = fg.chooseTasks(0, Collections.singletonList(StreamlineEventImpl.builder().fieldsAndValues(keyValues).dataSourceId("srcID").build()));
        Assert.assertEquals(1, res.size());
        Assert.assertTrue(res.get(0) == 0 || res.get(0) == 1);
    }

}