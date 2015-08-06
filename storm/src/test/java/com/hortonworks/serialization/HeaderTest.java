package com.hortonworks.serialization;

import com.hortonworks.bolt.Header;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * Created by pbrahmbhatt on 7/30/15.
 */
public class HeaderTest {

    @Test
    public void testSerialization() {
        Header header = new Header("nest", 1l);
        ByteBuffer buffer = ByteBuffer.allocate(header.size());
        header.writeHeader(buffer);
        byte[] bytes = buffer.array();

        buffer = ByteBuffer.wrap(bytes);
        Header header1 = Header.readHeader(buffer);
        Assert.assertEquals(header.getDeviceId(), header1.getDeviceId());
        Assert.assertEquals(header.getVersion(), header1.getVersion());
    }
}
