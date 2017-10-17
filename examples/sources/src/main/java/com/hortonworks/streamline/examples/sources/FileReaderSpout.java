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
package com.hortonworks.streamline.examples.sources;

import com.hortonworks.streamline.streams.StreamlineEvent;
import com.hortonworks.streamline.streams.common.StreamlineEventImpl;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FileReaderSpout extends BaseRichSpout {
    protected static final Logger LOG = LoggerFactory.getLogger(FileReaderSpout.class);
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private transient SpoutOutputCollector spoutOutputCollector;
    private final String path;
    private String delimiter;
    private String outputStream;

    public FileReaderSpout (String path) {
        this.path = path;
    }

    public FileReaderSpout withDelimiter (String delimiter) {
        if (delimiter == null || delimiter.isEmpty()) {
            String errorString = "Delimiter must not be empty";
            LOG.error(errorString);
            throw new RuntimeException(errorString);
        }
        this.delimiter = delimiter;
        return this;
    }

    public FileReaderSpout withOutputStream (String outputStream) {
        if (outputStream == null || outputStream.isEmpty()) {
            String errorString = "Output stream must not be empty";
            LOG.error(errorString);
            throw new RuntimeException(errorString);
        }
        this.outputStream = outputStream;
        return this;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declareStream(outputStream, new Fields(StreamlineEvent.STREAMLINE_EVENT));
    }

    @Override
    public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
        LOG.info("Opening FileReaderSpout");
        this.spoutOutputCollector = spoutOutputCollector;
    }

    @Override
    public void nextTuple() {
        try (BufferedReader br = new BufferedReader(Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] result = line.split(delimiter);
                if (result.length != 2) {
                   LOG.error("Format of input file not as expected. Expecting {} separated first name and last name", delimiter);
                } else {
                    List<Object> values = new ArrayList<>();
                    values.add(StreamlineEventImpl.builder().sourceStream(outputStream).put(FIRST_NAME, result[0].trim()).put(LAST_NAME, result[1].trim())
                            .build());
                    spoutOutputCollector.emit(outputStream, values, UUID.randomUUID());
                }
            }
        } catch (IOException e) {
            LOG.error("Got exception while reading file at {}", path);
            throw new RuntimeException(e);
        }
    }


}
