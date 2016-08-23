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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.hortonworks.iotas.streams.runtime.storm.bolt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.catalog.DataSource;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.client.CatalogRestClient;
import com.hortonworks.iotas.common.Constants;
import com.hortonworks.iotas.streams.IotasEvent;
import com.hortonworks.iotas.common.IotasEventImpl;
import com.hortonworks.iotas.model.IotasMessage;
import com.hortonworks.iotas.parsers.Parser;
import com.hortonworks.iotas.common.util.ProxyUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


public class ParserBolt extends BaseRichBolt {
    public static final String LOCAL_PARSER_JAR_PATH = "local.parser.jar.path";
    public static final String BYTES_FIELD = "bytes";
    public static final String PARSED_TUPLES_STREAM = "parsed_tuples_stream";
    public static final String FAILED_TO_PARSE_TUPLES_STREAM = "failed_to_parse_tuples_stream";


    private static final Logger LOG = LoggerFactory.getLogger(ParserBolt.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Random RANDOM = new Random();

    private static ConcurrentHashMap<Object, Parser> dataSrcIdfToParser = new ConcurrentHashMap<>();      //TODO why is this field static ? It makes the class really hard to test and takes away from the thread safety of storm bolts
    private static ConcurrentHashMap<Object, DataSource> dataSrcIdfToDataSrc = new ConcurrentHashMap<>(); //TODO why is this field static ? It makes the class really hard to test and takes away from the thread safety of storm bolts

    private CatalogRestClient client;
    private String localParserJarPath;
    private Long parserId;
    private String parsedTuplesStreamId;
    private String unparsedTuplesStreamId;
    private Long dataSourceId;
    private ProxyUtil<Parser> parserProxyUtil;

    private OutputCollector collector;

    /**
     * If user knows this instance of parserBolt is mapped to a topic which has messages that conforms to exactly one type of parser,
     * they can ignore the rest lookup to get parser based on deviceId and version and instead set a single parser via this method.
     *
     * @param parserId
     */
    public void withParserId(long parserId) {
        this.parserId = parserId;
    }

    /**
     * The stream id to use for emitting the tuples that were successfully
     * parsed
     * @param parsedTuplesStreamId
     */
    public void withParsedTuplesStreamId (String parsedTuplesStreamId) {
        this.parsedTuplesStreamId = parsedTuplesStreamId;
    }

    /**
     * The stream id to use for emitting the tuples that could not be parsed
     * @param unparsedTuplesStreamId
     */
    public void withUnparsedTuplesStreamId (String unparsedTuplesStreamId) {
        this.unparsedTuplesStreamId = unparsedTuplesStreamId;
    }

    /**
     * If the user knows the dataSourceId they can set it here. This will be set in the IotasEvent as the default.
     *
     * @param dataSourceId
     */
    public void withDataSourceId(long dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        if (!stormConf.containsKey(Constants.CATALOG_ROOT_URL) || !stormConf.containsKey(LOCAL_PARSER_JAR_PATH)) {
            throw new IllegalArgumentException("conf must contain " + Constants.CATALOG_ROOT_URL + " and " + LOCAL_PARSER_JAR_PATH);
        }
        String catalogRootURL = stormConf.get(Constants.CATALOG_ROOT_URL).toString();
        //We could also add the iotasMessage timestamp to calculate overall pipeline latency.
        this.collector = collector;
        this.localParserJarPath = stormConf.get(LOCAL_PARSER_JAR_PATH).toString();
        this.client = new CatalogRestClient(catalogRootURL);
        if (StringUtils.isEmpty(this.parsedTuplesStreamId)) {
            throw new IllegalStateException("Stream id must be defined for successfully parsed tuples");
        }
        this.parserProxyUtil = new ProxyUtil<>(Parser.class);
    }

    public void execute(Tuple input) {
        byte[] inputBytes = input.getBinaryByField(BYTES_FIELD);
        byte[] failedBytes = inputBytes;
        Parser parser = null;
        String messageId = null;
        try {
            if (parserId == null) {
                //If a parserId is not configured in parser Bolt, we assume that the input is an iotasMessage encoded in JSON
                final IotasMessage iotasMessage = objectMapper.readValue(new String(inputBytes, StandardCharsets.UTF_8), IotasMessage.class);
                parser = getParser(iotasMessage);
                if(dataSourceId == null) {
                    dataSourceId = getDataSource(iotasMessage).getId();
                }
                // override inputBytes with the data in IotasMessage
                inputBytes = iotasMessage.getData();
                messageId = iotasMessage.getMessageId();
            } else {
                parser = getParser(parserId);
            }

            // Checks if raw data is valid. Throws DataValidationException if raw data is invalid
            parser.validate(inputBytes);

            final Map<String, Object> parsedInput = parser.parse(inputBytes);

            // Checks if parsed data is valid. Throws DataValidationException if raw data is invalid
            parser.validate(parsedInput);

            final String dtSrcId = dataSourceId == null ? StringUtils.EMPTY : dataSourceId.toString();
            IotasEvent event;

            // If message id is set in the incoming message, we use it as the IotasEvent id, else the id is random UUID.
            if(messageId == null) {
                event = new IotasEventImpl(parsedInput, dtSrcId);
            } else {
                event = new IotasEventImpl(parsedInput, dtSrcId, messageId);
            }

            final Values values = new Values(event);
            collector.emit(parsedTuplesStreamId, input, values);
            collector.ack(input);
        } catch (Exception e) {
            if (unparsedTuplesStreamId != null) {
                LOG.warn("Failed to parse a tuple. Sending it to unparsed tuples stream " + unparsedTuplesStreamId, e);
                collector.emit(unparsedTuplesStreamId, input, new Values(failedBytes));
                collector.ack(input);
            } else {
                collector.fail(input);
                collector.reportError(e);
                LOG.error("Failed to parse a tuple and no stream defined for unparsed tuples.", e);
            }
        }
    }

    private Parser loadParser(ParserInfo parserInfo) {
        try {
            File parserJar = createLocalParserJar(parserInfo);

            try (InputStream parserJarInputStream = client.getParserJar(parserInfo.getId());
                 FileOutputStream fileOutputStream = new FileOutputStream(parserJar)) {
                IOUtils.copy(parserJarInputStream, fileOutputStream);
                return parserProxyUtil.loadClassFromJar(parserJar.getAbsolutePath(), parserInfo.getClassName());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load parser: " + parserInfo.getJarStoragePath(), e);
        }
    }

    private File createLocalParserJar(ParserInfo parserInfo) throws IOException {
        ensureDirExists(localParserJarPath);

        File parserJar = File.createTempFile(parserInfo.getName().trim(), "", new File(localParserJarPath));
        parserJar.deleteOnExit();

        return parserJar;
    }

    private void ensureDirExists(String localParserJarDir) throws IOException {
        File file = new File(localParserJarDir);
        if(file.exists() ) {
            if(!file.isDirectory()) {
                throw new IOException("Given path ["+localParserJarDir+"] is not a directory.");
            }
        } else if(!file.mkdirs()) {
            throw new IOException("Failed to create directory ["+localParserJarDir+"]");
        }
    }

    private DataSource getDataSource(IotasMessage iotasMessage) {
        DataSourceIdentifier dataSrcIdf = new DataSourceIdentifier(iotasMessage.getMake(), iotasMessage.getModel());
        DataSource dataSource = dataSrcIdfToDataSrc.get(dataSrcIdf);
        if(dataSource == null) {
            dataSource = client.getDataSource(dataSrcIdf.getId(), dataSrcIdf.getVersion());
            DataSource existing = dataSrcIdfToDataSrc.putIfAbsent(dataSrcIdf, dataSource);
            if(existing != null) {
                dataSource = existing;
            }
        }
        return dataSource;
    }

    private Parser getParser(IotasMessage iotasMessage) {
        DataSourceIdentifier dataSrcIdf = new DataSourceIdentifier(iotasMessage.getMake(), iotasMessage.getModel());
        Parser parser = dataSrcIdfToParser.get(dataSrcIdf);
        if (parser == null) {
            ParserInfo parserInfo = client.getParserInfo(dataSrcIdf.getId(), dataSrcIdf.getVersion());
            parser = getParserAndCacheIfAbsent(parserInfo, dataSrcIdf);
        }
        return parser;
    }

    private Parser getParser(Long parserId) {
        Parser parser = dataSrcIdfToParser.get(parserId);
        if (parser == null) {
            ParserInfo parserInfo = client.getParserInfo(parserId);
            parser = getParserAndCacheIfAbsent(parserInfo, parserId);
        }
        return parser;
    }

    private Parser getParserAndCacheIfAbsent(ParserInfo parserInfo, Object dataSourceId) {
        Parser loadedParser = loadParser(parserInfo);
        Parser parser = dataSrcIdfToParser.putIfAbsent(dataSourceId, loadedParser);
        if (parser == null) {
            parser = loadedParser;
        }
        return parser;
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declareStream(this.parsedTuplesStreamId, new Fields(IotasEvent.IOTAS_EVENT));
        if (this.unparsedTuplesStreamId != null) {
            declarer.declareStream(this.unparsedTuplesStreamId, new Fields(BYTES_FIELD));
        }
    }

    /**
     * Parser will always receive an IotasMessage, which will have id and version to uniquely identify the datasource
     * this message is associated with. This class is just a composite structure to represent that unique datasource identifier.
     */
    private static class DataSourceIdentifier {
        private String id;
        private String version;

        private DataSourceIdentifier(String id, String version) {
            this.id = id;
            this.version = version;
        }

        public String getId() {
            return id;
        }

        public String getVersion() {
            return version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DataSourceIdentifier)) return false;

            DataSourceIdentifier that = (DataSourceIdentifier) o;

            if (id != null ? !id.equals(that.id) : that.id != null) return false;
            return !(version != null ? !version.equals(that.version) : that.version != null);

        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (version != null ? version.hashCode() : 0);
            return result;
        }
    }
}
