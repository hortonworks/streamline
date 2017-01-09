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

