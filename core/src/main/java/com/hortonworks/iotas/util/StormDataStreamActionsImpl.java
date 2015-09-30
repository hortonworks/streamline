package com.hortonworks.iotas.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.iotas.catalog.DataStream;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.xml.crypto.Data;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Storm implementation of the DataStreamsAction interface
 */
public class StormDataStreamActionsImpl implements DataStreamActions {

    // artifact
    private static final String STORM_ARTIFACTS_LOCATION_KEY =
            "stormArtifactsDirectory";
    private static final String STORM_JAR_LOCATION_KEY = "iotasStormJar";
    private static final String YAML_KEY_NAME = "name";
    private static final String YAML_KEY_CATALOG_ROOT_URL = "catalog.root.url";
    private static final String YAML_KEY_LOCAL_PARSER_JAR_PATH = "local" +
            ".parser.jar.path";
    // TODO: add hbase conf to topology config when processing data sinks
    private static final String YAML_KEY_CONFIG = "config";
    private static final String YAML_KEY_HBASE_CONF = "hbase.conf";
    private static final String YAML_KEY_HBASE_ROOT_DIR = "hbase.root.dir";
    private static final String YAML_KEY_COMPONENTS = "components";
    private static final String YAML_KEY_SPOUTS = "spouts";
    private static final String YAML_KEY_BOLTS = "bolts";
    private static final String YAML_KEY_STREAMS = "streams";
    private static final String YAML_KEY_ID = "id";
    private static final String YAML_KEY_CLASS_NAME = "className";
    private static final String YAML_KEY_CONSTRUCTOR_ARGS = "constructorArgs";
    private static final String YAML_KEY_REF = "ref";
    private static final String YAML_KEY_ARGS = "args";
    private static final String YAML_KEY_CONFIG_METHODS = "configMethods";
    private static final String YAML_KEY_FROM = "from";
    private static final String YAML_KEY_TO = "to";
    private static final String YAML_KEY_GROUPING = "grouping";
    private static final String YAML_KEY_TYPE = "type";

    private String stormArtifactsLocation = "/tmp/storm-artifacts/";
    private Map<String, Object> yamlData;
    private  Map jsonMap;
    private DataStream dataStream;

    // Below 3 instance variables needed to hack the good bad tuples rule
    // configuration in json since it cant be handled by RuleBolt
    private String goodBadTuplesProcessor;
    private String goodTuplesRule;
    private String badTuplesRule;

    private Map<String, String> jsonUiNameToStormName = new HashMap<>();

    private String stormJarLocation;

    //TODO: Remove this wil unparsed tuple handler implementaion change
    private String parserBoltId;

    public StormDataStreamActionsImpl() {
    }

    @Override
    public void init (Map<String, String> conf) {
        if (conf != null) {
            if (conf.containsKey(STORM_ARTIFACTS_LOCATION_KEY)) {
                stormArtifactsLocation = conf.get(STORM_ARTIFACTS_LOCATION_KEY);
            }
            stormJarLocation = conf.get(STORM_JAR_LOCATION_KEY);
        }
        File f = new File (stormArtifactsLocation);
        f.mkdirs();
    }

    @Override
    public void deploy (DataStream dataStream) throws Exception {
        String fileName = this.createYamlFile(dataStream);
        List<String> commands = new ArrayList<String>();
        commands.add("storm");
        commands.add("jar");
        commands.add(stormJarLocation);
        commands.add("org.apache.storm.flux.Flux");
        commands.add("--remote");
        commands.add(fileName);
        int exitValue = executeShellProcess(commands);
        if (exitValue != 0) {
            throw new Exception("Datastream could not be deployed " +
                    "successfylly.");
        }
    }

    @Override
    public void kill (DataStream dataStream) throws Exception {
        List<String> commands = new ArrayList<String>();
        commands.add("storm");
        commands.add("kill");
        commands.add(getTopologyName(dataStream));
        int exitValue = executeShellProcess(commands);
        if (exitValue != 0) {
            throw new Exception("Datastream could not be killed " +
                    "successfylly.");
        }
    }

    synchronized private String createYamlFile (DataStream dataStream) throws
            Exception {
        this.reset();
        this.dataStream = dataStream;
        String json = this.dataStream.getJson();
        ObjectMapper objectMapper = new ObjectMapper();
        File f;
        FileWriter fileWriter = null;
        try {
            f = new File(this.getFilePath());
            if (f.exists()) {
                if (!f.delete()) {
                    throw new Exception("Unable to delete old storm " +
                            "artifact for data stream id " + this.dataStream
                            .getDataStreamId());
                }
            }

            jsonMap = objectMapper.readValue(json, Map.class);
            yamlData = new LinkedHashMap<String, Object>();

            yamlData.put(YAML_KEY_NAME, this.getTopologyName(dataStream));
            addTopologyConfig();
            addDataSources();
            addDataSinks();
            addProcessors();
            addLinks();
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            //options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
            Yaml yaml = new Yaml (options);
            fileWriter = new FileWriter(f);
            yaml.dump(yamlData, fileWriter);
            return f.getAbsolutePath();
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }

    private String getTopologyName (DataStream dataStream) {
        return "iotas-" + dataStream.getDataStreamId();
    }
    private String getFilePath () {
        return this.stormArtifactsLocation + "dataStreamId-" + this.dataStream
                .getDataStreamId() + ".yaml";
    }

    private void addTopologyConfig () {
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put(YAML_KEY_CATALOG_ROOT_URL, jsonMap.get
                (DataStreamLayoutValidator.JSON_KEY_CATALOG_ROOT_URL));
        config.put(YAML_KEY_LOCAL_PARSER_JAR_PATH, "/tmp"); //TODO
        yamlData.put(YAML_KEY_CONFIG, config);
    }

    private void addDataSources () {
       List<Map> dataSources = (List<Map>) jsonMap.get(
               DataStreamLayoutValidator.JSON_KEY_DATA_SOURCES);
        for (Map dataSource: dataSources) {
            switch (DataStreamLayoutValidator.DataSourceType.valueOf
                    ((String) dataSource.get(DataStreamLayoutValidator
                            .JSON_KEY_TYPE))) {
                case KAFKA:
                    this.addKafkaDataSource(dataSource);
                    break;
                default:
                    throw new RuntimeException("Invalid data source type in " +
                            "data stream layout");
            }
        }
        return;
    }

    private void addDataSinks () {
       List<Map> dataSinks = (List<Map>) jsonMap.get(
               DataStreamLayoutValidator.JSON_KEY_DATA_SINKS);
        for (Map dataSink: dataSinks) {
            switch (DataStreamLayoutValidator.DataSinkType.valueOf
                    ((String) dataSink.get(DataStreamLayoutValidator
                            .JSON_KEY_TYPE))) {
                case HBASE:
                    this.addHbaseDataSink(dataSink);
                    break;
                case HDFS:
                    this.addHdfsDataSink(dataSink);
                    break;
                default:
                    throw new RuntimeException("Invalid data sink type in " +
                            "data stream layout");
            }
        }
        // TODO: below code to add printer bolt only to make it work for demo.
        // Should be removed later as printer bolt will not be configured in
        // json data stream layout created by user
        String printerBoltId = "PrinterBolt";
        String printerBoltClassName = "com.hortonworks.bolt.PrinterBolt";
        this.addToBolts(this.createComponent(printerBoltId,
                printerBoltClassName, null, null));

        //TODO: hack for demo where we need to connect PrinterBolt to
        // ParserBolt without it being represented in UI json
        this.jsonUiNameToStormName.put(printerBoltId, printerBoltId);
        this.jsonUiNameToStormName.put(parserBoltId, parserBoltId);
        Map grouping = new LinkedHashMap();
        grouping.put(YAML_KEY_TYPE, "SHUFFLE");
        this.addToStreams(this.createStream(parserBoltId, printerBoltId,
                grouping));
        return;
    }

    private void addProcessors () {
       List<Map> processors = (List<Map>) jsonMap.get(
               DataStreamLayoutValidator.JSON_KEY_PROCESSORS);
        for (Map processor: processors) {
            List<Map> processorComponents = (List) processor.get
                    (DataStreamLayoutValidator.JSON_KEY_CONFIG);
            //TODO: the below if block is present to handle the good and bad
            // tuples rule. configuration. Hacky for now. May be change it if
            // we can incorporate it in rules engine
            if (processorComponents.size() == 2) {
                Map processorComponent1 = (Map) processorComponents.get(0);
                Map processorComponent2 = (Map) processorComponents.get(1);
                DataStreamLayoutValidator.ProcessorType processorType1 =
                        DataStreamLayoutValidator.ProcessorType.valueOf(
                                (String) processorComponent1.get
                                        (DataStreamLayoutValidator.JSON_KEY_TYPE));
                DataStreamLayoutValidator.ProcessorType processorType2 =
                        DataStreamLayoutValidator.ProcessorType.valueOf(
                                (String) processorComponent2.get
                                        (DataStreamLayoutValidator.JSON_KEY_TYPE));
                if (processorType1 == DataStreamLayoutValidator.ProcessorType
                        .RULE && processorType2 == DataStreamLayoutValidator
                        .ProcessorType.RULE) {
                    Integer ruleId1 = (Integer) processorComponent1.get
                            (DataStreamLayoutValidator.JSON_KEY_ID);
                    Integer ruleId2 = (Integer) processorComponent2.get
                            (DataStreamLayoutValidator.JSON_KEY_ID);
                    //TODO: Assuming the ids are 1 and 2. Need to change
                    // after rules catalog is designed
                    if (ruleId1.equals(1) && ruleId2.equals(2)) {
                        this.goodBadTuplesProcessor = (String) processor.get
                                (DataStreamLayoutValidator.JSON_KEY_UINAME);
                        this.goodTuplesRule = (String) processorComponent1.get
                                (DataStreamLayoutValidator.JSON_KEY_UINAME);
                        this.badTuplesRule = (String) processorComponent2.get
                                (DataStreamLayoutValidator.JSON_KEY_UINAME);
                        this.jsonUiNameToStormName.put(goodTuplesRule,
                                parserBoltId);
                        continue;
                    }
                }
            }

            for (Map processorComponent: processorComponents) {
                switch (DataStreamLayoutValidator.ProcessorType.valueOf
                        ((String) processorComponent.get(
                                DataStreamLayoutValidator.JSON_KEY_TYPE))) {
                    case RULE:
                        this.addRule(processorComponent);
                        break;
                    default:
                        throw new RuntimeException("Invalid processor type in " +
                                "data stream layout");
                }
            }
        }
        return;
    }

    private void addLinks () {
       List<Map> links = (List<Map>) jsonMap.get(
               DataStreamLayoutValidator.JSON_KEY_LINKS);
        for (Map link: links) {
            String linkFrom = (String) link.get(DataStreamLayoutValidator
                    .JSON_KEY_FROM);
            String linkTo = (String) link.get(DataStreamLayoutValidator
                    .JSON_KEY_TO);
            if (linkTo.equals(goodBadTuplesProcessor)) {
                //special case for good bad tuples processor. do nothing
                continue;
            }
            if (linkFrom.equals(badTuplesRule)) {
                //special case for bad tuples rule
                continue;
            }
            Map grouping = new LinkedHashMap();
            grouping.put(YAML_KEY_TYPE, "SHUFFLE");
            this.addToStreams(this.createStream(linkFrom, linkTo, grouping));
        }
        return;
    }

    private void addKafkaDataSource (Map dataSource) {
        // uiname needs to be unique - enterd by user
        String dataSourceUiName  = (String) dataSource.get
                (DataStreamLayoutValidator.JSON_KEY_UINAME);
        Map config = (Map) dataSource.get(DataStreamLayoutValidator
                .JSON_KEY_CONFIG);

        String zkHostsComponentId = dataSourceUiName + "ZkHosts";
        String zkHostsClassName = "storm.kafka.ZkHosts";
        List zkHostsConstructorArgs = new ArrayList();
        zkHostsConstructorArgs.add(config.get(DataStreamLayoutValidator
                .JSON_KEY_ZK_URL));
        this.addToComponents(this.createComponent(zkHostsComponentId,
                zkHostsClassName, zkHostsConstructorArgs, null));

        String spoutConfigId = dataSourceUiName + "SpoutConfig";
        String spoutConfigClassName = "storm.kafka.SpoutConfig";
        List spoutConfigConstructorArgs = new ArrayList();
        Map ref = new LinkedHashMap();
        ref.put(YAML_KEY_REF, zkHostsComponentId);
        spoutConfigConstructorArgs.add(ref);
        spoutConfigConstructorArgs.add(config.get(DataStreamLayoutValidator
                .JSON_KEY_TOPIC));
        //TODO for zkRoot and and may be id which are next two constructor args
        // not using uiname for data source below as it may have spaces and
        // zkRoot needs to be unique across data streams
        spoutConfigConstructorArgs.add("/Iotas-kafka-spout-" + this
                .dataStream.getDataStreamId() + "-" + dataSource.get
                (DataStreamLayoutValidator.JSON_KEY_ID));
        spoutConfigConstructorArgs.add("kafka-spout-" + this
                .dataStream.getDataStreamId() + "-" + dataSource.get
                (DataStreamLayoutValidator.JSON_KEY_ID));
        this.addToComponents(this.createComponent(spoutConfigId,
                spoutConfigClassName, spoutConfigConstructorArgs, null));

        String kafkaSpoutId = dataSourceUiName + "KafkaSpout";
        String kafkaSpoutClassName = "storm.kafka.KafkaSpout";
        List kafkaSpoutConstructorArgs = new ArrayList();
        ref = new LinkedHashMap();
        ref.put(YAML_KEY_REF, spoutConfigId);
        kafkaSpoutConstructorArgs.add(ref);
        this.addToSpouts(this.createComponent(kafkaSpoutId,
                kafkaSpoutClassName, kafkaSpoutConstructorArgs, null));

        //When we have a kafka spout as data source we add a parser bolt by
        // default. Hence the below code
        String parserBoltId = dataSourceUiName + "ParserBolt";
        this.parserBoltId = parserBoltId;
        String parserBoltClassName = "com.hortonworks.bolt.ParserBolt";
        this.addToBolts(this.createComponent(parserBoltId,
                parserBoltClassName, null, null));

        //TODO: hack for special case - to connect spout to parser bolt
        // without a link in the ui and hence json
        this.jsonUiNameToStormName.put(dataSourceUiName, kafkaSpoutId);
        this.jsonUiNameToStormName.put(parserBoltId, parserBoltId);
        Map grouping = new LinkedHashMap();
        grouping.put(YAML_KEY_TYPE, "SHUFFLE");
        this.addToStreams(this.createStream(dataSourceUiName, parserBoltId,
                grouping));
    }

    private void addHbaseDataSink (Map dataSink) {
        // uiname needs to be unique - enterd by user
        String dataSinkUiName  = (String) dataSink.get
                (DataStreamLayoutValidator.JSON_KEY_UINAME);
        Map config = (Map) dataSink.get(DataStreamLayoutValidator
                .JSON_KEY_CONFIG);

        String hbaseMapperComponentId = dataSinkUiName + "HbaseMapper";
        String hbaseMapperClassName = "com.hortonworks.hbase.ParserOutputHBaseMapper";
        List hbaseMapperConstructorArgs = new ArrayList();
        hbaseMapperConstructorArgs.add(config.get(DataStreamLayoutValidator
                .JSON_KEY_COLUMN_FAMILY));
        this.addToComponents(this.createComponent(hbaseMapperComponentId,
                hbaseMapperClassName, hbaseMapperConstructorArgs, null));

        String hbaseBoltId = dataSinkUiName + "HbaseBolt";
        String hbaseBoltClassName = "org.apache.storm.hbase.bolt.HBaseBolt";
        List hbaseBoltConstructorArgs = new ArrayList();
        hbaseBoltConstructorArgs.add(config.get(DataStreamLayoutValidator
                .JSON_KEY_TABLE));
        Map ref = new LinkedHashMap();
        ref.put(YAML_KEY_REF, hbaseMapperComponentId);
        hbaseBoltConstructorArgs.add(ref);
        List hbaseBoltConfigMethods = new ArrayList();
        Map configKeyMethod = new LinkedHashMap();
        configKeyMethod.put(YAML_KEY_NAME, "withConfigKey");
        List configKeyMethodArgs = new ArrayList();
        configKeyMethodArgs.add(YAML_KEY_HBASE_CONF);
        configKeyMethod.put(YAML_KEY_ARGS, configKeyMethodArgs);
        hbaseBoltConfigMethods.add(configKeyMethod);
        this.addToBolts(this.createComponent(hbaseBoltId,
                hbaseBoltClassName, hbaseBoltConstructorArgs,
                hbaseBoltConfigMethods));
        this.jsonUiNameToStormName.put(dataSinkUiName, hbaseBoltId);
        // TODO: kind of hacky but thats how it hbase bolt currently works
        // may be change it to pass it directly to bolt rather than through
        // topology config. same with rowKey. currently there is no way for
        // row key to be configurable. Need to be able to do that
        Map topologyConfig = (LinkedHashMap) this.yamlData.get(YAML_KEY_CONFIG);
        if (topologyConfig == null) {
            topologyConfig = new LinkedHashMap();
            this.yamlData.put(YAML_KEY_CONFIG, topologyConfig);
        }
        Map hbaseConfMap = new LinkedHashMap();
        hbaseConfMap.put(YAML_KEY_HBASE_ROOT_DIR, (String) config.get
                (DataStreamLayoutValidator.JSON_KEY_ROOT_DIR));
        topologyConfig.put(YAML_KEY_HBASE_CONF, hbaseConfMap);
    }

    private void addHdfsDataSink (Map dataSink) {
        //TODO: currently hdfs sink is only used for unparsed tuple handler
        // from parser bolt. Need to change that to good and bad stream. And
        // change this method to create a hdfs bolt component rather than
        // HdfsUnparsedTupleHandler component
        // uiname needs to be unique - enterd by user
        String dataSinkUiName  = (String) dataSink.get
                (DataStreamLayoutValidator.JSON_KEY_UINAME);
        Map config = (Map) dataSink.get(DataStreamLayoutValidator
                .JSON_KEY_CONFIG);

        String hdfsComponentId = dataSinkUiName + "UnparsedTupleHandler";
        String hdfsComponentClassName = "com.hortonworks.topology" +
                ".HdfsUnparsedTupleHandler";

        List hdfsComponentConfigMethods = new ArrayList();

        Map fsurlConfigMethod = new LinkedHashMap();
        fsurlConfigMethod.put(YAML_KEY_NAME, "withFsUrl");
        List fsurlMethodArgs = new ArrayList();
        fsurlMethodArgs.add(config.get(DataStreamLayoutValidator
                .JSON_KEY_FS_URL));
        fsurlConfigMethod.put(YAML_KEY_ARGS, fsurlMethodArgs);
        hdfsComponentConfigMethods.add(fsurlConfigMethod);

        Map pathConfigMethod = new LinkedHashMap();
        pathConfigMethod.put(YAML_KEY_NAME, "withPath");
        List pathMethodArgs = new ArrayList();
        pathMethodArgs.add(config.get(DataStreamLayoutValidator
                .JSON_KEY_PATH));
        pathConfigMethod.put(YAML_KEY_ARGS, pathMethodArgs);
        hdfsComponentConfigMethods.add(pathConfigMethod);

        Map nameConfigMethod = new LinkedHashMap();
        nameConfigMethod.put(YAML_KEY_NAME, "withName");
        List nameMethodArgs = new ArrayList();
        nameMethodArgs.add(config.get(DataStreamLayoutValidator
                .JSON_KEY_NAME));
        nameConfigMethod.put(YAML_KEY_ARGS, nameMethodArgs);
        hdfsComponentConfigMethods.add(nameConfigMethod);

        //TODO: figure out where the below config should come from. Hard
        // coding for now
        Map rotationIntervalConfigMethod = new LinkedHashMap();
        rotationIntervalConfigMethod.put(YAML_KEY_NAME, "withRotationInterval");
        List rotationIntervalMethodArgs = new ArrayList();
        rotationIntervalMethodArgs.add(1);
        rotationIntervalConfigMethod.put(YAML_KEY_ARGS,
                rotationIntervalMethodArgs);
        hdfsComponentConfigMethods.add(rotationIntervalConfigMethod);


        this.addToComponents(this.createComponent(hdfsComponentId,
                hdfsComponentClassName, null, hdfsComponentConfigMethods));

        // TODO: below is also throw away code where we add the unparsed
        // tuple handler to ParserBolt. Once we change the hdfs unparsed
        // tuple handler implementation to a bolt we will remove the below
        // code and wire it up using links
        List<Map> bolts = (List) this.yamlData.get(YAML_KEY_BOLTS);
        for (Map bolt: bolts) {
            String id = (String) bolt.get("id");
            if (id.equals(this.parserBoltId)) {
                List parserBoltConfigMethods = new ArrayList();
                Map tupleHandlerConfigMethod = new LinkedHashMap();
                tupleHandlerConfigMethod.put(YAML_KEY_NAME,
                        "withUnparsedTupleHandler");
                List tupleHandlerMethodArgs = new ArrayList();
                Map ref = new LinkedHashMap();
                ref.put(YAML_KEY_REF, hdfsComponentId);
                tupleHandlerMethodArgs.add(ref);
                tupleHandlerConfigMethod.put(YAML_KEY_ARGS,
                        tupleHandlerMethodArgs);
                parserBoltConfigMethods.add(tupleHandlerConfigMethod);
                bolt.put(YAML_KEY_CONFIG_METHODS, parserBoltConfigMethods);
                break;
            }
        }
    }

    private void addRule (Map rule) {
        //TODO: add code to create rule bolt here when rule engine
        // implementation is done
        return;
    }

    private Map createComponent (String id, String className, List
            constructorArgs, List configMethods) {
        Map component = new LinkedHashMap();
        component.put(YAML_KEY_ID, id);
        component.put(YAML_KEY_CLASS_NAME, className);
        if (constructorArgs != null && constructorArgs.size() > 0) {
            component.put(YAML_KEY_CONSTRUCTOR_ARGS, constructorArgs);
        }
        if (configMethods != null && configMethods.size() > 0) {
            component.put(YAML_KEY_CONFIG_METHODS, configMethods);
        }
        return component;
    }

    private Map createStream (String from, String to, Map grouping) {
        Map stream = new LinkedHashMap();
        from = this.jsonUiNameToStormName.get(from);
        to = this.jsonUiNameToStormName.get(to);
        stream.put(YAML_KEY_NAME, from + "->" + to);
        stream.put(YAML_KEY_FROM, from);
        stream.put(YAML_KEY_TO, to);
        stream.put(YAML_KEY_GROUPING, grouping);
        return stream;
    }
    private void addToComponents (Map component) {
        this.addComponentToCollection(component, YAML_KEY_COMPONENTS);
    }

    private void addToSpouts (Map spout) {
        this.addComponentToCollection(spout, YAML_KEY_SPOUTS);
    }

    private void addToBolts (Map bolt) {
        this.addComponentToCollection(bolt, YAML_KEY_BOLTS);
    }

    private void addToStreams (Map stream) {
        this.addComponentToCollection(stream, YAML_KEY_STREAMS);
    }

    private void addComponentToCollection (Map component, String
            collectionKey) {
        if (component == null ) {
            return;
        }
        List components = (ArrayList) yamlData.get(collectionKey);
        if (components == null) {
            components = new ArrayList();
            yamlData.put(collectionKey, components);
        }
        components.add(component);
    }

    private void reset () {
        jsonUiNameToStormName.clear();
        // reset the below 3 for special good bad tuples rule
        this.goodBadTuplesProcessor = "";
        this.goodTuplesRule = "";
        this.badTuplesRule = "";

    }

    private int executeShellProcess (List<String> commands) throws  Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.inheritIO();
        Process process = processBuilder.start();
        process.waitFor();
        int exitValue = process.exitValue();
        System.out.println("Exit value from subprocess is :" + exitValue);
        return exitValue;
    }

}
