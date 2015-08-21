package com.hortonworks.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.client.RestClient;
import com.hortonworks.iotas.catalog.ParserInfo;
import com.hortonworks.iotas.model.IotasMessage;
import com.hortonworks.iotas.parser.Parser;
import com.hortonworks.util.ReflectionHelper;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ParserBolt extends BaseRichBolt {
    public static final String CATALOG_ROOT_URL = "catalog.root.url";
    public static final String LOCAL_PARSER_JAR_PATH = "local.parser.jar.path";
    private OutputCollector collector;
    private RestClient client;
    private String localParserJarPath;
    private static ConcurrentHashMap<DataSourceIdentifier, Parser> dataSourceToParserMap = new ConcurrentHashMap<DataSourceIdentifier, Parser>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private Fields outputFields;

    public ParserBolt(Fields outputFields) {
        this.outputFields = outputFields;
    }

    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        if (!stormConf.containsKey(CATALOG_ROOT_URL) || !stormConf.containsKey(LOCAL_PARSER_JAR_PATH)) {
            throw new IllegalArgumentException("conf must contain " + CATALOG_ROOT_URL + " and " + LOCAL_PARSER_JAR_PATH);
        }
        String catalogRootURL = stormConf.get(CATALOG_ROOT_URL).toString();
        //TODO May be we should always add the id, version, type and metadata from iotasMessage as output fields?
        //We could also add the iotasMessage timestamp to calculate overall pipeline latency.
        this.collector = collector;
        this.localParserJarPath = stormConf.get(LOCAL_PARSER_JAR_PATH).toString();
        this.client = new RestClient(catalogRootURL);
    }

    public void execute(Tuple input) {
        byte[] bytes = input.getBinaryByField("bytes");
        try {
            IotasMessage iotasMessage = objectMapper.readValue(new String(bytes, StandardCharsets.UTF_8), IotasMessage.class);
            Parser parser = getParser(iotasMessage);
            Map<String, Object> parsed = parser.parse(iotasMessage.getData());
            Values values = new Values();
            for (String str : outputFields) {
                values.add(parsed.get(str));
            }
            collector.emit(input, values);
            collector.ack(input);
        } catch (Exception e) {
            collector.fail(input);
            collector.reportError(e);
        }
    }

    private Parser loadParser(DataSourceIdentifier dataSourceId) {
        //TODO: We need to agree on version's type, string is probably a better choice.
        ParserInfo parserInfo = client.getParserInfo(dataSourceId.getId(), Long.valueOf(dataSourceId.getVersion()));
        InputStream parserJar = client.getParserJar(parserInfo.getParserId());
        String jarPath = String.format("%s%s%s-%s.jar", localParserJarPath, File.pathSeparator, dataSourceId.getId(), dataSourceId.getVersion());

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

    private Parser getParser(IotasMessage iotasMessage) {
        DataSourceIdentifier dataSourceId = new DataSourceIdentifier(iotasMessage.getId(), iotasMessage.getVersion());
        Parser parser = dataSourceToParserMap.get(dataSourceId);
        if (parser == null) {
            Parser loadedParser = loadParser(dataSourceId);
            parser = dataSourceToParserMap.putIfAbsent(dataSourceId, loadedParser);
            if (parser == null) {
                parser = loadedParser;
            }
        }
        return parser;
    }


    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(outputFields);
    }

    /**
     * Parser will always receive an IotasMessage, which will have id and version to uniquely identify the datasource
     * this message is associated with. This class is just a composite structure to represent that unique datasource identifier.
     */
    private static class DataSourceIdentifier {
        private String id;
        private Long version;

        private DataSourceIdentifier(String id, Long version) {
            this.id =  id;
            this.version = version;
        }

        public String getId() {
            return id;
        }

        public Long getVersion() {
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
