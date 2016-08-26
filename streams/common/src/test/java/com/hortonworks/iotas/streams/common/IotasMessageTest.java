package com.hortonworks.iotas.streams.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.streams.common.IotasMessage;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Created by aiyer on 9/24/15.
 */
public class IotasMessageTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testMappingWithoutMessageId() throws IOException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("iotas-message-1.json");

        IotasMessage iotasMessage = objectMapper.readValue(in, IotasMessage.class);

        assertEquals(iotasMessage.getMake(), "nest");
        assertNull(iotasMessage.getMessageId());
    }

    @Test
    public void testMappingWithMessageId() throws IOException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("iotas-message-2.json");

        IotasMessage iotasMessage = objectMapper.readValue(in, IotasMessage.class);

        assertEquals(iotasMessage.getMake(), "nest");
        assertEquals(iotasMessage.getMessageId(), "100");
    }
}