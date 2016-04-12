package com.hortonworks.iotas.hdfs;

import org.apache.storm.tuple.Tuple;
import com.hortonworks.iotas.bolt.ParserBolt;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(JMockit.class)
public class IdentityHdfsRecordFormatTest {

    private static final byte[] TEST_BYTES = "test-bytes".getBytes();
    private static final byte[] TEST_BYTES_RESULT = "test-bytes\n".getBytes();

    private IdentityHdfsRecordFormat format = new IdentityHdfsRecordFormat();
    private @Mocked Tuple mockTuple;

    @Before
    public void setup () {
        new Expectations() {{
            mockTuple.getBinaryByField(ParserBolt.BYTES_FIELD); returns(TEST_BYTES);
        }};
    }

    @Test
    public void testFormat () {
        byte[] bytes = format.format(mockTuple);
        Assert.assertTrue(Arrays.equals(TEST_BYTES_RESULT, bytes));
    }

}

