package com.hortonworks.streamline.streams.runtime.storm.hbase;

import com.google.common.base.Charsets;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.storm.hbase.common.ColumnList;
import org.apache.storm.tuple.Tuple;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Charsets.UTF_8;

@RunWith(JMockit.class)
public class StreamlineEventHBaseMapperTest {

    private static final String ROW_KEY_FIELD = "rowKey";
    private static final String COLUMN_FAMILY= "columnFamily";
    private static final String COLUMN_FIELD= "columnField";
    private static final Map<String, Object> TEST_PARSED_MAP = new HashMap<String, Object>() {{
       put(COLUMN_FIELD, COLUMN_FIELD);
    }};
    private static final StreamlineEvent TEST_EVENT = new StreamlineEventImpl(TEST_PARSED_MAP, "dsrcid1", ROW_KEY_FIELD);


    private StreamlineEventHBaseMapper mapper = new StreamlineEventHBaseMapper(COLUMN_FAMILY);
    private @Mocked Tuple mockTuple;

    @Before
    public void setup() {
        new Expectations() {{
            mockTuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT); returns(TEST_EVENT);
        }};
    }

    @Test
    public void testMapper() {
        byte[] bytes = mapper.rowKey(mockTuple);
        Assert.assertTrue(Arrays.equals(ROW_KEY_FIELD.getBytes(UTF_8), bytes));

        ColumnList columns = mapper.columns(mockTuple);
        Assert.assertEquals(1, columns.getColumns().size());
        ColumnList.Column column = columns.getColumns().get(0);
        Assert.assertTrue(Arrays.equals(COLUMN_FAMILY.getBytes(Charsets.UTF_8), column.getFamily()));
        Assert.assertTrue(Arrays.equals(COLUMN_FIELD.getBytes(Charsets.UTF_8), column.getQualifier()));
        Assert.assertTrue(Arrays.equals(COLUMN_FIELD.getBytes(Charsets.UTF_8), column.getValue()));
    }

}
