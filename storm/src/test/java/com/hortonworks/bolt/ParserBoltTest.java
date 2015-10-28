package com.hortonworks.bolt;

import backtype.storm.Config;
import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.hortonworks.client.CatalogRestClient;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.model.IotasMessage;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;

@RunWith(JMockit.class)
public class ParserBoltTest {

    private static final Long PARSER_ID = 1l;
    private static final String DEVICE_ID = "1";
    private static final Long VERSION = 1l;
    private static final byte[] DATA = "test".getBytes(Charsets.UTF_8);
    private static final Values VALUES = new Values(MockParser.IOTAS_EVENT);
    private static final String PARSED_TUPLES_STREAM = "parsed_tuples_stream";
    private static final String FAILED_TO_PARSE_TUPLES_STREAM =
            "failed_to_parse_tuples_stream";

    private IotasMessage msg;
    private @Tested ParserBolt parserBolt;
    private ParserInfo parserInfo;

    private @Injectable OutputCollector mockOutputCollector;
    private @Injectable
    CatalogRestClient mockClient;
    private @Injectable Tuple mockTuple;

    @Before
    public void setup() throws Exception {
        msg = new IotasMessage();
        parserBolt = new ParserBolt();
        parserBolt.withParsedTuplesStreamId(PARSED_TUPLES_STREAM);
        parserBolt.withUnparsedTuplesStreamId(FAILED_TO_PARSE_TUPLES_STREAM);
        parserInfo = new ParserInfo();

        msg.setId(DEVICE_ID);
        msg.setVersion(VERSION);
        msg.setData(DATA);

        Config config = new Config();
        config.put(ParserBolt.CATALOG_ROOT_URL, "test");
        config.put(ParserBolt.LOCAL_PARSER_JAR_PATH, "/tmp");
        parserBolt.prepare(config, null, mockOutputCollector);
        parserBolt.setClient(mockClient);

        parserInfo.setParserId(PARSER_ID);
        parserInfo.setClassName(MockParser.class.getCanonicalName());
        parserInfo.setParserName("TestParser");
    }

    @Test
    public void testParserBoltHandlesIotasMessages() throws Exception {
        final byte[] json = new ObjectMapper().writeValueAsString(msg).getBytes(Charsets.UTF_8);
        new Expectations() {{
            mockTuple.getBinaryByField(ParserBolt.BINARY_BYTES); returns(json);
            mockClient.getParserJar(PARSER_ID); result = new ByteArrayInputStream("test-stream".getBytes());
            mockClient.getParserInfo(DEVICE_ID, VERSION); result = parserInfo;
        }};

        callExecuteAndVerifyCollectorInteraction(true);
    }

    @Test
    public void testBadMessage() throws Exception {
        final byte[] json = "bad-iotas-message".getBytes();
        new Expectations() {{
            mockTuple.getBinaryByField(ParserBolt.BINARY_BYTES); returns(json);
        }};

        callExecuteAndVerifyCollectorInteraction(false);
    }

    @Test
    public void testParserBoltHandlesNonIotasMessage() throws Exception {
        parserBolt.withParserId(PARSER_ID);

        new Expectations() {{
            mockTuple.getBinaryByField(ParserBolt.BINARY_BYTES); returns(DATA);
            mockClient.getParserJar(PARSER_ID); returns(new ByteArrayInputStream("test-stream".getBytes()));
            mockClient.getParserInfo(PARSER_ID); returns(parserInfo);
        }};

        callExecuteAndVerifyCollectorInteraction(true);
    }

    private void callExecuteAndVerifyCollectorInteraction(boolean isSuccess) {
        parserBolt.execute(mockTuple);

        if(isSuccess) {
            new VerificationsInOrder() {{
                mockOutputCollector.emit(PARSED_TUPLES_STREAM, mockTuple, withAny
                        (VALUES));
                mockOutputCollector.ack(mockTuple);
            }};

        } else {
            new VerificationsInOrder() {{
                mockOutputCollector.emit(FAILED_TO_PARSE_TUPLES_STREAM, mockTuple, withAny
                        (VALUES));
                mockOutputCollector.ack(mockTuple);
            }};
        }
    }

}
