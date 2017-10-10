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
        BufferedReader br = null;
        try {
            String line;
            br = new BufferedReader(Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8));
            while ((line = br.readLine()) != null) {
                String[] result = line.split(delimiter);
                if (result.length != 2) {
                   LOG.error("Format of input file not as expected. Expecting {} separated first name and last name", delimiter);
                } else {
                    Map<String, Object> event = new HashMap<>();
                    event.put(FIRST_NAME, result[0].trim());
                    event.put(LAST_NAME, result[1].trim());
                    List<Object> values = new ArrayList<>();
                    values.add(new MyStreamlineEvent(event, outputStream));
                    spoutOutputCollector.emit(outputStream, values, UUID.randomUUID());
                }
            }
        } catch (IOException e) {
            LOG.error("Got exception while reading file at {}", path);
            throw new RuntimeException(e);
        } finally {
            if (br != null) try {
                br.close();
            } catch (IOException e) {
                LOG.error("Got exception while closing buffered reader");
                throw new RuntimeException(e);
            }
        }

    }

    private static class MyStreamlineEvent implements StreamlineEvent {

        private final Map<String, Object> unmodifiableMap;
        private final String outputStream;
        private MyStreamlineEvent (Map<String, Object> fields, String outputStream) {
            unmodifiableMap = Collections.unmodifiableMap(fields);
            this.outputStream = outputStream;
        }

        @Override
        public Map<String, Object> getAuxiliaryFieldsAndValues() {
            return null;
        }

        @Override
        public void addAuxiliaryFieldAndValue(String field, Object value) {

        }

        @Override
        public Map<String, Object> getHeader() {
            return null;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getDataSourceId() {
            return null;
        }

        @Override
        public String getSourceStream() {
            return outputStream;
        }

        @Override
        public StreamlineEvent addFieldsAndValues(Map<String, Object> fieldsAndValues) {
            Map<String, Object> newFieldsAndValues = new HashMap<>();
            newFieldsAndValues.putAll(unmodifiableMap);
            newFieldsAndValues.putAll(fieldsAndValues);
            return new MyStreamlineEvent(newFieldsAndValues, this.outputStream);
        }

        @Override
        public StreamlineEvent addFieldAndValue(String key, Object value) {
            Map<String, Object> fieldsAndValues = new HashMap<>();
            fieldsAndValues.put(key, value);
            fieldsAndValues.putAll(unmodifiableMap);
            return new MyStreamlineEvent(fieldsAndValues, this.outputStream);
        }

        @Override
        public StreamlineEvent addHeaders(Map<String, Object> headers) {
            return this;
        }

        @Override
        public byte[] getBytes() {
            return new byte[0];
        }

        @Override
        public int size() {
            return unmodifiableMap.size();
        }

        @Override
        public boolean isEmpty() {
            return unmodifiableMap.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return unmodifiableMap.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return unmodifiableMap.containsValue(value);
        }

        @Override
        public Object get(Object key) {
            return unmodifiableMap.get(key);
        }

        @Override
        public Set<String> keySet() {
            return unmodifiableMap.keySet();
        }

        @Override
        public Collection<Object> values() {
            return unmodifiableMap.values();
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return unmodifiableMap.entrySet();
        }
    }
}
