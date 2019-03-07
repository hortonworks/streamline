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
package com.hortonworks.streamline.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link Config}
 */
public class ConfigTest {

    Config config;

    @Before
    public void setUp() throws Exception {
        String json = "{\"properties\": {\"a\":\"1\", \"b\":\"hello\", \"c\":\"true\"}}";
        ObjectMapper mapper = new ObjectMapper();
        config = mapper.readValue(json, Config.class);
    }

    @Test
    public void testGet() throws Exception {
        //System.out.println(config);
        assertEquals(1, config.getInt("a"));
        assertEquals(10, config.getInt("aa", 10));
        assertEquals(true, config.getBoolean("c"));
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetEx() throws Exception {
        //System.out.println(config);
        assertEquals(1, config.getInt("aa"));
    }


    @Test
    public void testPut() throws Exception {
        config.put("a", 1000);
        config.put("d", "world");
        assertEquals(1000, config.getInt("a"));
        assertEquals("world", config.get("d"));
    }

    @Test
    public void testFromProperties() throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("config.properties");
        properties.load(inputStream);
        Config cfg = new Config(properties);
        assertEquals(100, cfg.getInt("a"));
        assertEquals("hello", cfg.get("b"));
        assertTrue(cfg.getBoolean("c"));
    }

    @Test
    public void testFromPropertiesFile() throws IOException {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("config.properties");
        Config cfg = new Config(inputStream);
        assertEquals(100, cfg.getInt("a"));
        assertEquals("hello", cfg.get("b"));
        assertTrue(cfg.getBoolean("c"));
    }
}