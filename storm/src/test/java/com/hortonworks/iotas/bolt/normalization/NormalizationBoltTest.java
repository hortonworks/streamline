/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.bolt.normalization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.common.Schema;
import com.hortonworks.iotas.layout.design.component.NormalizationProcessor;
import com.hortonworks.iotas.layout.design.component.Stream;
import com.hortonworks.iotas.layout.design.normalization.*;
import com.hortonworks.iotas.layout.runtime.normalization.NormalizationException;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.IOUtils;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
@RunWith(JMockit.class)
public class NormalizationBoltTest {
    private static final Logger log = LoggerFactory.getLogger(NormalizationBoltTest.class);

    @Injectable
    private TopologyContext topologyContext;

    @Injectable
    private OutputCollector outputCollector;

    @Injectable
    private OutputFieldsDeclarer outputFieldsDeclarer;

    @Injectable
    private Tuple tuple;

    public static final IotasEventImpl INPUT_IOTAS_EVENT = new IotasEventImpl(new HashMap<String, Object>() {{
        put("illuminance", 70);
        put("temp", 104);
        put("foo", 100);
        put("humidity", "40h");
    }}, "ds-" + System.currentTimeMillis(), "id-" + System.currentTimeMillis());

    public static final IotasEventImpl INVALID_INPUT_IOTAS_EVENT = new IotasEventImpl(new HashMap<String, Object>() {{
        put("illuminance", 70);
        put("tmprtr", 101);
        put("foo", 100);
        put("humidity", "40");
    }}, "ds-" + System.currentTimeMillis(), "id-" + System.currentTimeMillis());

    private static final Schema OUTPUT_SCHEMA_FIELDS = Schema.of(
            new Schema.Field("temperature", Schema.Type.FLOAT),
            new Schema.Field("humidity", Schema.Type.STRING),
            new Schema.Field("illuminance", Schema.Type.INTEGER),
            new Schema.Field("new-field", Schema.Type.STRING));

    private static final IotasEvent VALID_OUTPUT_IOTAS_EVENT =
            new IotasEventImpl(new HashMap<String, Object>() {{
                put("temperature", 40);
                put("humidity", "40h");
                put("illuminance", 70);
                put("new-field", "new value");
            }}, INPUT_IOTAS_EVENT.getDataSourceId(), INPUT_IOTAS_EVENT.getId());

    @Test
    public void testFieldBasedNormalization() throws NormalizationException {
        testNormalizationBolt(createNormalizationBolt(createFieldBasedNormalizationProcessorRuntime()));
    }

    @Test
    public void testFieldBasedNormalizationFailure() throws NormalizationException {
        testNormalizationBoltFailure(createNormalizationBolt(createFieldBasedNormalizationProcessorRuntime()));
    }

    @Test
    public void testBulkNormalization() throws NormalizationException, IOException {
        testNormalizationBolt(createNormalizationBolt(createBulkNormalizationProcessorRuntime()));
    }

    @Test
    public void testBulkNormalizationFailure() throws NormalizationException, IOException {
        testNormalizationBoltFailure(createNormalizationBolt(createBulkNormalizationProcessorRuntime()));
    }

    private void testNormalizationBolt(NormalizationBolt normalizationBolt) {
        new Expectations() {{
            tuple.getValueByField(IotasEvent.IOTAS_EVENT);
            returns(INPUT_IOTAS_EVENT);
        }};

        normalizationBolt.execute(tuple);

        new Verifications() {{
            outputCollector.emit(withAny(""), tuple, new Values(VALID_OUTPUT_IOTAS_EVENT));
            times = 1;
            outputCollector.ack(tuple);
        }};
    }

    private void testNormalizationBoltFailure(NormalizationBolt normalizationBolt) {
        new Expectations() {{
            tuple.getValueByField(IotasEvent.IOTAS_EVENT);
            returns(INVALID_INPUT_IOTAS_EVENT);
        }};

        normalizationBolt.execute(tuple);

        new Verifications() {{
            outputCollector.emit(withAny(""), withAny(tuple), withAny(new Values())); times = 0;
            outputCollector.fail(tuple); times = 1;
            outputCollector.reportError(withAny(new IllegalArgumentException())); times = 1;
        }};
    }

    private NormalizationBolt createNormalizationBolt(NormalizationProcessor normalizationProcessor) throws NormalizationException {
        NormalizationBolt normalizationBolt = new NormalizationBolt(normalizationProcessor);

        normalizationBolt.declareOutputFields(outputFieldsDeclarer);
        normalizationBolt.prepare(null, topologyContext, outputCollector);
        return normalizationBolt;
    }

    private NormalizationProcessor createBulkNormalizationProcessorRuntime() throws NormalizationException, IOException {
        Map<String, NormalizationConfig> inputStreamsWithConfig = new HashMap<>();
        Schema.Field tempField = new Schema.Field("temp", Schema.Type.INTEGER);
        Schema inputSchema = Schema.of(tempField, new Schema.Field("foo", Schema.Type.STRING));

        String bulkScriptText = getBulkScriptText();
        BulkNormalizationConfig bulkNormalizationConfig = new BulkNormalizationConfig(inputSchema, bulkScriptText);
        inputStreamsWithConfig.put(NormalizationProcessor.DEFAULT_STREAM_ID, bulkNormalizationConfig);

        NormalizationProcessor normalizationProcessor = new NormalizationProcessor(inputStreamsWithConfig, new Stream("normalized-output", OUTPUT_SCHEMA_FIELDS),  NormalizationProcessor.Type.bulk);

        return normalizationProcessor;
    }

    private String getBulkScriptText() throws IOException {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("normalization/bulkNormalizationScript.groovy")) {
            return IOUtils.toString(is);
        }
    }

    private NormalizationProcessor createFieldBasedNormalizationProcessorRuntime() throws NormalizationException {
        Map<String, NormalizationConfig> inputStreamsWithConfig = new HashMap<>();
        Schema.Field tempField = new Schema.Field("temp", Schema.Type.INTEGER);
        Schema inputSchema = Schema.of(tempField, new Schema.Field("foo", Schema.Type.STRING));

        Transformer transformer = new Transformer(tempField, new Schema.Field("temperature", Schema.Type.FLOAT));
        transformer.setConverterScript("new Float((temp-32)*5/9f)");

        List<Transformer> transformers = Collections.singletonList(transformer);
        List<String> filters = Collections.singletonList("foo");
        List<FieldValueGenerator> fieldValueGenerators = Collections.singletonList(new FieldValueGenerator(new Schema.Field("new-field", Schema.Type.STRING), "new value"));
        FieldBasedNormalizationConfig fieldBasedNormalizationConfig = new FieldBasedNormalizationConfig(inputSchema, transformers, filters, fieldValueGenerators);

        inputStreamsWithConfig.put(NormalizationProcessor.DEFAULT_STREAM_ID, fieldBasedNormalizationConfig);

        NormalizationProcessor normalizationProcessor = new NormalizationProcessor(inputStreamsWithConfig, new Stream("normalized-output", OUTPUT_SCHEMA_FIELDS), NormalizationProcessor.Type.fineGrained);
        return normalizationProcessor;
    }


    @Test
    public void testBulkNormalizationProcessorJsons() throws Exception {
        testNormalizationProcessorJson(createBulkNormalizationProcessorRuntime());
    }

    @Test
    public void testFinegrainedNormalizationProcessorJsons() throws Exception {
        testNormalizationProcessorJson(createFieldBasedNormalizationProcessorRuntime());
    }

    private void testNormalizationProcessorJson(NormalizationProcessor normalizationProcessor) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final String normalizationProcessorJson = objectMapper.writeValueAsString(normalizationProcessor);
        log.info("####### normalizationProcessorJson = " + normalizationProcessorJson);

        final NormalizationProcessor readNormalizationProcessor = objectMapper.readValue(normalizationProcessorJson, NormalizationProcessor.class);
        Assert.assertEquals(normalizationProcessor, readNormalizationProcessor);

        final String jsonFromGeneratedObject = objectMapper.writeValueAsString(readNormalizationProcessor);
        log.info("####### jsonFromGeneratedObject = " + jsonFromGeneratedObject);

        Assert.assertEquals(normalizationProcessorJson, jsonFromGeneratedObject);
    }

    @Test
    public void testGroovyWithCustomObjects() throws IOException {
        Binding binding = new Binding();
        binding.setVariable("device", new Device("device-"+System.currentTimeMillis()));
        for (Map.Entry<String, Object> entry : INPUT_IOTAS_EVENT.getFieldsAndValues().entrySet()) {
            binding.setVariable(entry.getKey(), entry.getValue());
        }
        binding.setVariable("__outputSchema", OUTPUT_SCHEMA_FIELDS);

        GroovyShell groovyShell = new GroovyShell(binding);

        Object result = groovyShell.evaluate(getBulkScriptText());
    }

    static class Device {
        String deviceName;

        Device(String deviceName) {
            this.deviceName = deviceName;
        }

        public void setName(String name) {
            this.deviceName = name;
        }

        public float convertToCelsius(float inFahrenheit) {
            return (inFahrenheit - 32) * 5 / 9f;
        }

        @Override
        public String toString() {
            return "Device{" +
                    "deviceName='" + deviceName + '\'' +
                    '}';
        }
    }

}
