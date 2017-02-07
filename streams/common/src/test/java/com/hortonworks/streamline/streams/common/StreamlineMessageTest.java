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
package com.hortonworks.streamline.streams.common;

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
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("streamline-message-1.json");

        StreamlineMessage message = objectMapper.readValue(in, StreamlineMessage.class);

        assertEquals(message.getMake(), "nest");
        assertNull(message.getMessageId());
    }

    @Test
    public void testMappingWithMessageId() throws IOException {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("streamline-message-2.json");

        StreamlineMessage message = objectMapper.readValue(in, StreamlineMessage.class);

        assertEquals(message.getMake(), "nest");
        assertEquals(message.getMessageId(), "100");
    }
}