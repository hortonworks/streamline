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
package com.hortonworks.streamline.streams.runtime.storm.bolt;

import com.google.common.collect.ImmutableMap;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.TupleImpl;
import org.apache.storm.tuple.Values;
import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit test for {@link StreamsShellBoltTest}
 */
@RunWith(JMockit.class)
public class StreamsShellBoltTest {

    @Mocked
    OutputCollector mockCollector;

    @Mocked
    TopologyContext mockContext;

    @Test
    public void testStreamsShellBoltTest() throws Exception {
        setUpExpectations();
        copyFiles(readFile("/splitsentence.py") , new File("/tmp/splitsentence.py"));
        copyFiles(readFile("/streamline.py"), new File("/tmp/streamline.py"));
        String command = "python splitsentence.py";
        StreamsShellBolt streamsShellBolt = new StreamsShellBolt(command, 60000);
        streamsShellBolt = streamsShellBolt.withOutputStreams(Arrays.asList("stream1"));

        streamsShellBolt.prepare(new HashMap(), mockContext, mockCollector);
        streamsShellBolt.execute(getNextTuple(1));
        new Verifications() {
            {
                String streamId;
                Tuple anchor;
                List<List<Object>> tuples = new ArrayList<>();
                mockCollector.emit(streamId = withCapture(), anchor = withCapture(), withCapture(tuples));
                Assert.assertEquals("stream", streamId);
                Assert.assertEquals(4, tuples.size());
                Map<String, Object> fieldsAndValues = ((StreamlineEvent) tuples.get(0).get(0));
                Assert.assertEquals("THIS", fieldsAndValues.get("word"));
                fieldsAndValues = ((StreamlineEvent) tuples.get(1).get(0));
                Assert.assertEquals("IS", fieldsAndValues.get("word"));
                fieldsAndValues = ((StreamlineEvent) tuples.get(2).get(0));
                Assert.assertEquals("RANDOM", fieldsAndValues.get("word"));
                fieldsAndValues = ((StreamlineEvent) tuples.get(3).get(0));
                Assert.assertEquals("SENTENCE1", fieldsAndValues.get("word"));
            }
        };
    }

    private static void copyFiles(InputStream is, File dest) throws IOException {
        OutputStream os = null;
        try {
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            os.close();
            os.close();
        }
    }
    private void setUpExpectations() {
        new Expectations() {{
            mockContext.getComponentOutputFields(anyString, anyString);
            result = new Fields(StreamlineEvent.STREAMLINE_EVENT);
            mockContext.getComponentId(anyInt);
            result = "1-componentid";
            mockContext.getPIDDir();
            result = "/tmp";
            mockContext.getCodeDir();
            result = "/tmp";
            mockContext.getThisComponentId();
            result = "1-componentid"; minTimes = 0;
        }};
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNullCommand() throws Exception {
        StreamsShellBolt streamsShellBolt = new StreamsShellBolt(null, 60000);
    }

    private Tuple getNextTuple(int i) {
        StreamlineEvent event = StreamlineEventImpl.builder().fieldsAndValues(
                ImmutableMap.of("sentence", "THIS IS RANDOM SENTENCE"+ i)
        ).dataSourceId("dsrcid").build();
        return new TupleImpl(mockContext, new Values(event), 1, "inputstream");
    }

    private InputStream readFile(String fn) throws IOException {
        return getClass().getResourceAsStream(fn);
    }

}