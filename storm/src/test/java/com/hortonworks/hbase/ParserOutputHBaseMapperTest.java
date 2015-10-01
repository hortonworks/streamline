package com.hortonworks.hbase;

import backtype.storm.tuple.Tuple;
import com.google.common.base.Charsets;
import com.hortonworks.bolt.ParserBolt;
import mockit.Expectations;
import mockit.Mocked;
import org.apache.storm.hbase.common.ColumnList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Charsets.UTF_8;

public class ParserOutputHBaseMapperTest {

    private static final String ROW_KEY_FIELD = "rowKey";
    private static final String COLUMN_FAMILY= "columnFamily";
    private static final String COLUMN_FIELD= "columnField";
    private static final Map<String, Object> TEST_PARSED_MAP = new HashMap<String, Object>() {{
       put(ROW_KEY_FIELD, ROW_KEY_FIELD);
       put(COLUMN_FIELD, COLUMN_FIELD);
    }};


    private ParserOutputHBaseMapper mapper = new ParserOutputHBaseMapper(ROW_KEY_FIELD, COLUMN_FAMILY);
    private @Mocked Tuple mockTuple;

    @Before
    public void setup() {
        new Expectations() {{
            mockTuple.getValueByField(ParserBolt.PARSED_FIELDS); returns(TEST_PARSED_MAP);
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
