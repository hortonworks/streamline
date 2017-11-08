/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
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

    private static final String COLUMN_FAMILY= "columnFamily";
    private static final String COLUMN_FIELD= "columnField";
    private static final Map<String, Object> TEST_PARSED_MAP = new HashMap<String, Object>() {{
       put(COLUMN_FIELD, COLUMN_FIELD);
    }};
    private static final StreamlineEvent TEST_EVENT = StreamlineEventImpl.builder()
            .fieldsAndValues(TEST_PARSED_MAP)
            .dataSourceId("dsrcid1")
            .build();


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
        Assert.assertTrue(Arrays.equals(TEST_EVENT.getId().getBytes(UTF_8), bytes));

        ColumnList columns = mapper.columns(mockTuple);
        Assert.assertEquals(1, columns.getColumns().size());
        ColumnList.Column column = columns.getColumns().get(0);
        Assert.assertTrue(Arrays.equals(COLUMN_FAMILY.getBytes(Charsets.UTF_8), column.getFamily()));
        Assert.assertTrue(Arrays.equals(COLUMN_FIELD.getBytes(Charsets.UTF_8), column.getQualifier()));
        Assert.assertTrue(Arrays.equals(COLUMN_FIELD.getBytes(Charsets.UTF_8), column.getValue()));
    }

}
