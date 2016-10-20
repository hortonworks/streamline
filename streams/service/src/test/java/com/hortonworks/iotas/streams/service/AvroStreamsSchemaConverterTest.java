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
package com.hortonworks.iotas.streams.service;

import org.apache.commons.io.IOUtils;
import org.apache.streamline.streams.service.AvroStreamsSchemaConverter;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class AvroStreamsSchemaConverterTest {
    private static final Logger LOG = LoggerFactory.getLogger(AvroStreamsSchemaConverterTest.class);

    @Test
    public void testAvroToStreamsSchemaConversion() throws Exception {
        _testAvroToStreamsSchemaConversion("/schemas/device.avsc", "/schemas/device.stsc");
        _testAvroToStreamsSchemaConversion("/schemas/device-prim.avsc", "/schemas/device-prim.stsc");
    }

    private void _testAvroToStreamsSchemaConversion(String avroSchemaLoc, String streamsSchemaLoc) throws IOException {
        AvroStreamsSchemaConverter avroStreamsSchemaConverter = new AvroStreamsSchemaConverter();
        try (InputStream deviceAvroSchemaStream = AvroStreamsSchemaConverterTest.class.getResourceAsStream(avroSchemaLoc);
             InputStream deviceStreamsSchemaStream = AvroStreamsSchemaConverterTest.class.getResourceAsStream(streamsSchemaLoc)) {
            String avroSchema = IOUtils.toString(deviceAvroSchemaStream);
            String streamsSchema = avroStreamsSchemaConverter.convertAvro(avroSchema);

            LOG.info("streamsSchema = #{}#", streamsSchema);

            Assert.assertEquals(IOUtils.toString(deviceStreamsSchemaStream), streamsSchema);
        }
    }
}
