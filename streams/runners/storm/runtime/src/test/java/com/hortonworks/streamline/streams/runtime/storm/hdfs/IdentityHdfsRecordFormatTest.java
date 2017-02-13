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
package com.hortonworks.streamline.streams.runtime.storm.hdfs;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.storm.tuple.Tuple;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashMap;

@RunWith(JMockit.class)
public class IdentityHdfsRecordFormatTest {

    private static final StreamlineEvent STREAMLINEEVENT = new StreamlineEventImpl(new HashMap<>(), "id");
    private static final byte[] TEST_BYTES_RESULT = (STREAMLINEEVENT.toString() + "\n").getBytes();

    private IdentityHdfsRecordFormat format = new IdentityHdfsRecordFormat();
    private @Mocked Tuple mockTuple;

    @Before
    public void setup () {
        new Expectations() {{
            mockTuple.getValueByField(StreamlineEvent.STREAMLINE_EVENT); returns(STREAMLINEEVENT);
        }};
    }

    @Test
    public void testFormat () {
        byte[] bytes = format.format(mockTuple);
        Assert.assertTrue(Arrays.equals(TEST_BYTES_RESULT, bytes));
    }

}

