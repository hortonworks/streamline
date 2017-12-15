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
package com.hortonworks.streamline.streams.runtime.storm.spout;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class AvroKafkaSpoutTranslatorTest {

    @Test
    public void testByteBufferInputStream() throws Exception {
        int res = read(1000, 1000, 0, 1000);
        assertEquals(1000, res);
    }

    @Test
    public void testByteBufferInputStreamNonZeroOffset1() throws Exception {
        int res = read(1000, 1000, 100, 900);
        assertEquals(900, res);
    }

    @Test
    public void testByteBufferInputStreamNonZeroOffset2() throws Exception {
        int res = read(1000, 1000, 600, 400);
        assertEquals(400, res);
    }

    @Test
    public void testByteBufferInputStreamSmallBuffer() throws Exception {
        int res = read(100, 1000, 600, 400);
        assertEquals(100, res);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testByteBufferInputStreamSmallDest() throws Exception {
        int res = read(1000, 100, 600, 400);
        assertEquals(100, res);
    }

    private int read(int bufSize, int destSize, int offset, int length) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(bufSize);
        AvroKafkaSpoutTranslator.ByteBufferInputStream is = new AvroKafkaSpoutTranslator.ByteBufferInputStream(byteBuffer);
        byte[] dest = new byte[destSize];
        return is.read(dest, offset, length);
    }
}