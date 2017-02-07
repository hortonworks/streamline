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
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hortonworks.streamline.streams.service;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class AvroStreamlineSchemaConverterTest {
    private static final Logger LOG = LoggerFactory.getLogger(AvroStreamlineSchemaConverterTest.class);
    private static final String UTF_8 = "UTF-8";

    @Test
    public void testAvroToStreamsSchemaConversion() throws Exception {
        _testAvroToStreamsSchemaConversion("/schemas/device.avsc", "/schemas/device.stsc");
        _testAvroToStreamsSchemaConversion("/schemas/device-prim.avsc", "/schemas/device-prim.stsc");
    }

    @Test
    public void testStreamlineSchemaToAvroSchema() throws Exception {
        _testStreamlineSchemaToAvroSchemaConversion("/schemas/device.stsc","/schemas/device-gen.avsc");
    }

    private void _testStreamlineSchemaToAvroSchemaConversion(String streamslineSchemaLoc, String avroSchemaLoc) throws Exception {
        try (InputStream deviceAvroSchemaStream = AvroStreamlineSchemaConverterTest.class.getResourceAsStream(avroSchemaLoc);
             InputStream deviceStreamsSchemaStream = AvroStreamlineSchemaConverterTest.class.getResourceAsStream(streamslineSchemaLoc)) {

            String streamlineSchema = IOUtils.toString(deviceStreamsSchemaStream, UTF_8);
            String avroSchema = AvroStreamlineSchemaConverter.convertStreamlineSchemaToAvroSchema(streamlineSchema);

            LOG.info("avro schema = #{}#", avroSchema);

            Assert.assertEquals(IOUtils.toString(deviceAvroSchemaStream, UTF_8), avroSchema);
        }
    }

    private void _testAvroToStreamsSchemaConversion(String avroSchemaLoc, String streamsSchemaLoc) throws IOException {
        try (InputStream deviceAvroSchemaStream = AvroStreamlineSchemaConverterTest.class.getResourceAsStream(avroSchemaLoc);
             InputStream deviceStreamsSchemaStream = AvroStreamlineSchemaConverterTest.class.getResourceAsStream(streamsSchemaLoc)) {

            String avroSchema = IOUtils.toString(deviceAvroSchemaStream, UTF_8);
            String streamlineSchema = AvroStreamlineSchemaConverter.convertAvroSchemaToStreamlineSchema(avroSchema);

            LOG.info("streamlineSchema = #{}#", streamlineSchema);

            Assert.assertEquals(IOUtils.toString(deviceStreamsSchemaStream, UTF_8), streamlineSchema);
        }
    }
}
