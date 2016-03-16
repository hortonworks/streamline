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
package com.hortonworks.iotas.layout.runtime.normalization;

import com.hortonworks.iotas.common.IotasEvent;
import com.hortonworks.iotas.common.Result;
import com.hortonworks.iotas.common.errors.ProcessingException;
import com.hortonworks.iotas.layout.design.component.NormalizationProcessor;
import com.hortonworks.iotas.layout.design.component.Stream;
import com.hortonworks.iotas.layout.design.normalization.NormalizationConfig;
import com.hortonworks.iotas.processor.ProcessorRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 *
 */
public class NormalizationProcessorRuntime implements ProcessorRuntime {
    private static final Logger LOG = LoggerFactory.getLogger(NormalizationProcessorRuntime.class);

    // todo this map would be changed to Map<Stream, NormalizationRuntime> once we have support of streams on Processor
    // so that normalization can be done by taking input schema as respective stream's schema.
    private Map<String, NormalizationRuntime> schemasWithNormalizationRuntime;

    final NormalizationProcessor normalizationProcessor;

    public NormalizationProcessorRuntime(NormalizationProcessor normalizationProcessor) {
        this.normalizationProcessor = normalizationProcessor;
    }

    public Set<Stream> getOutputStream() {
        return normalizationProcessor.getDeclaredOutputStreams();
    }

    /*
     * todo: It should receive input Stream also and generate output Stream along with IotasEvent. This support should
     * come from processor framework, will add later.
     */
    @Override
    public List<Result> process(IotasEvent iotasEvent) throws ProcessingException {
        // todo receive input stream through IotasEvent, get respective normalization-runtime for that and execute it.
        // taking default stream for now.
        String currentStreamId = iotasEvent.getSourceStream() != null ? iotasEvent.getSourceStream() : NormalizationProcessor.DEFAULT_STREAM_ID;
        NormalizationRuntime normalizationRuntime = schemasWithNormalizationRuntime.get(currentStreamId);
        LOG.debug("Normalization runtime for this stream [{}]", normalizationRuntime);

        IotasEvent outputEvent = iotasEvent;
        if (normalizationRuntime != null) {
            try {
                outputEvent =  normalizationRuntime.execute(iotasEvent);
            } catch (NormalizationException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOG.debug("No normalization defined for stream: [{}]", currentStreamId);
        }

        // if it is not found return received tuple without any normalization, it is kind of pass through normalization for streams
        // which does not have normalization configuration.
        return Collections.singletonList(new Result(NormalizationProcessor.DEFAULT_STREAM_ID, Collections.singletonList(outputEvent)));
    }

    @Override
    public void initialize(Map<String, Object> config) {
        NormalizationRuntime.Factory factory = new NormalizationRuntime.Factory();
        Map<String, NormalizationRuntime> schemaRuntimes = new HashMap<>();
        for (Map.Entry<String, NormalizationConfig> entry : normalizationProcessor.inputStreamsWithConfig.entrySet()) {
            schemaRuntimes.put(entry.getKey(), factory.create(entry.getValue()));
        }
        schemasWithNormalizationRuntime = schemaRuntimes;
    }

    @Override
    public void cleanup() {

    }
}
