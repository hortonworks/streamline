package com.hortonworks.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.hortonworks.client.RestClient;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.parser.Parser;
import com.hortonworks.util.ReflectionHelper;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ParserBolt extends BaseRichBolt {
    public static final String CATALOG_ROOT_URL = "catalog.root.url";
    public static final String LOCAL_PARSER_JAR_PATH = "local.parser.jar.path";
    private OutputCollector collector;
    private RestClient client;
    private String localParserJarPath;
    private static ConcurrentHashMap<Header, Parser> headerToParserMap = new ConcurrentHashMap<Header, Parser>();

    //TODO, This should actually be coming from an admin or some config.
    private static final Fields outputFields = new Fields("userId", "temperature", "eventTime", "longitude", "latitude");

    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        if (!stormConf.containsKey(CATALOG_ROOT_URL) || !stormConf.containsKey(LOCAL_PARSER_JAR_PATH)) {
            throw new IllegalArgumentException("conf must contain " + CATALOG_ROOT_URL + " and " + LOCAL_PARSER_JAR_PATH);
        }
        String catalogRootURL = stormConf.get(CATALOG_ROOT_URL).toString();

        this.collector = collector;
        this.localParserJarPath = stormConf.get(LOCAL_PARSER_JAR_PATH).toString();
        this.client = new RestClient(catalogRootURL);
    }

    public void execute(Tuple input) {
        byte[] bytes = input.getBinaryByField("bytes");
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        Header header = Header.readHeader(buffer);
        Parser parser = getParser(header);
        try {
            //TODO: Should parser get the bytes with the HEADER? Given the header is assumed to be added not by vendor but some intermediate processor
            //IMO it should not be passed to parser.
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            Map<String, Object> parsed = parser.parse(data);
            Values values = new Values();
            for (String str : outputFields) {
                //TODO : assumes parser output contains keys for all the values.
                values.add(parsed.get(str));
            }
            collector.emit(input, values);
            collector.ack(input);
        } catch (Exception e) {
            collector.fail(input);
            collector.reportError(e);
        }
    }

    private Parser loadParser(Header header) {
        ParserInfo parserInfo = client.getParserInfo(header.getDeviceId(), header.getVersion());
        InputStream parserJar = client.getParserJar(parserInfo.getParserId());
        String jarPath = String.format("%s%s%s-%s.jar", localParserJarPath, File.pathSeparator, header.getDeviceId(), header.getVersion());

        try {
            IOUtils.copy(parserJar, new FileOutputStream(new File(jarPath)));
            if (!ReflectionHelper.isJarInClassPath(jarPath) && !ReflectionHelper.isClassLoaded(parserInfo.getClassName())) {
                ReflectionHelper.loadJarAndAllItsClasses(jarPath);
            }

            return ReflectionHelper.newInstance(parserInfo.getClassName());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Parser getParser(Header header) {
        Parser parser = headerToParserMap.get(header);
        if (parser == null) {
            Parser loadedParser = loadParser(header);
            parser = headerToParserMap.putIfAbsent(header, loadedParser);
            if (parser == null) {
                parser = loadedParser;
            }
        }
        return parser;
    }


    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(outputFields);
    }
}
