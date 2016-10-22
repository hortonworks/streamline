package org.apache.streamline.streams.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by aiyer on 9/24/15.
 */
public class StreamlineMessageTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testMappingWithoutMessageId() throws IOException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("iotas-message-1.json");

        StreamlineMessage message = objectMapper.readValue(in, StreamlineMessage.class);

        assertEquals(message.getMake(), "nest");
        assertNull(message.getMessageId());
    }

    @Test
    public void testMappingWithMessageId() throws IOException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("iotas-message-2.json");

        StreamlineMessage message = objectMapper.readValue(in, StreamlineMessage.class);

        assertEquals(message.getMake(), "nest");
        assertEquals(message.getMessageId(), "100");
    }
}