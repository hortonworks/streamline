package com.hortonworks.iotas.bolt;

import org.apache.storm.Config;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;

import com.hortonworks.iotas.client.CatalogRestClient;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.model.IotasMessage;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.VerificationsInOrder;
import mockit.integration.junit4.JMockit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ConcurrentHashMap;

import static com.hortonworks.iotas.bolt.ParserBolt.PARSED_TUPLES_STREAM;
import static com.hortonworks.iotas.bolt.ParserBolt.FAILED_TO_PARSE_TUPLES_STREAM;

@RunWith(JMockit.class)
public class ParserBoltTest {

    private static final Long PARSER_ID = 1L;
    private static final String DEVICE_ID = "1";
    private static final Long VERSION = 1L;
    private static final byte[] DATA = "test".getBytes(Charsets.UTF_8);
    private static final Values VALUES = new Values(MockParser.IOTAS_EVENT);

    private IotasMessage msg;
    private @Tested ParserBolt parserBolt;
    private ParserInfo parserInfo;

    private @Injectable OutputCollector mockOutputCollector;
    private @Mocked CatalogRestClient mockRestClient;
    private @Injectable Tuple mockTuple;

    @Before
    public void setup() throws Exception {
        parserBolt = new ParserBolt();
        parserBolt.withParsedTuplesStreamId(PARSED_TUPLES_STREAM);
        parserBolt.withUnparsedTuplesStreamId(FAILED_TO_PARSE_TUPLES_STREAM);

        Config config = new Config();
        config.put(ParserBolt.CATALOG_ROOT_URL, "test");
        config.put(ParserBolt.LOCAL_PARSER_JAR_PATH, "/tmp");

        parserBolt.prepare(config, null, mockOutputCollector);

        msg = new IotasMessage();
        msg.setId(DEVICE_ID);
        msg.setVersion(VERSION);
        msg.setData(DATA);

        parserInfo = new ParserInfo();
        parserInfo.setId(PARSER_ID);
        parserInfo.setClassName(MockParser.class.getCanonicalName());
        parserInfo.setName("TestParser");
    }

    @After
    public void tearDown() throws Exception {
        // This is necessary because the maps are static, and some methods are not
        // called depending on the order the tests are failed.
        mockit.Deencapsulation.setField(ParserBolt.class,"dataSrcIdfToParser", new ConcurrentHashMap<>());
        mockit.Deencapsulation.setField(ParserBolt.class,"dataSrcIdfToDataSrc", new ConcurrentHashMap<>());
    }

    @Test
    public void testParserBoltHandlesIotasMessages() throws Exception {
        final byte[] json = new ObjectMapper().writeValueAsString(msg).getBytes(Charsets.UTF_8);

        new Expectations() {{
            mockTuple.getBinaryByField(ParserBolt.BYTES_FIELD); returns(json);
            mockRestClient.getParserJar(PARSER_ID); result = new ByteArrayInputStream("test-stream".getBytes());
            mockRestClient.getParserInfo(DEVICE_ID, VERSION); result = parserInfo;
        }};

        callExecuteAndVerifyCollectorInteraction(true);
    }

    @Test
    public void testParserBoltHandlesNonIotasMessage() throws Exception {
        parserBolt.withParserId(PARSER_ID);

        new TupleRestClientExpectations(DATA);

        callExecuteAndVerifyCollectorInteraction(true);
    }

    @Test
    public void testBadMessage() throws Exception {
        final byte[] json = "bad-iotas-message".getBytes(); // throws exception because it is not in JSON format

        new Expectations() {{
            mockTuple.getBinaryByField(ParserBolt.BYTES_FIELD); returns(json);
        }};

        callExecuteAndVerifyCollectorInteraction(false);
    }

    @Test
    public void test_dataValidationException_failTuple() throws Exception {
        doTestDataValidation(MockBadParser.class.getCanonicalName(), false);
    }

    @Test
    public void test_validRawData_invalidParsedData_failTuple() throws Exception {
        doTestDataValidation(MockParser.ValidRawDataInvalidParsedDataParser.class.getName(), false);
    }

    @Test
    public void test_validData_emitTuple() throws Exception {
        doTestDataValidation(MockParser.ValidDataParser.class.getName(), true);
    }

    private void doTestDataValidation(String name, boolean success) {
        parserBolt.withParserId(PARSER_ID);
        parserInfo.setClassName(name);

        new TupleRestClientExpectations(DATA);

        callExecuteAndVerifyCollectorInteraction(success);
    }

    private void callExecuteAndVerifyCollectorInteraction(boolean success) {
        parserBolt.execute(mockTuple);

        if(success) {
            new VerificationsInOrder() {{
                mockOutputCollector.emit(PARSED_TUPLES_STREAM, mockTuple, withAny(VALUES));
                mockOutputCollector.ack(mockTuple);
            }};

        } else {
            new VerificationsInOrder() {{
                mockOutputCollector.emit(FAILED_TO_PARSE_TUPLES_STREAM, mockTuple, withAny(VALUES));
                mockOutputCollector.ack(mockTuple);
            }};
        }
    }

    private final class TupleRestClientExpectations extends Expectations {
        public TupleRestClientExpectations(byte[] data) {
            mockTuple.getBinaryByField(ParserBolt.BYTES_FIELD);
            returns(data);

            mockRestClient.getParserJar(PARSER_ID);
            returns(new ByteArrayInputStream("test-stream".getBytes()));

            mockRestClient.getParserInfo(PARSER_ID);
            returns(parserInfo);
        }
    }
}
