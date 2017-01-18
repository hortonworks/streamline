package com.hortonworks.streamline.streams.layout.storm;

public final class StormTopologyLayoutConstants {

    private StormTopologyLayoutConstants() {
    }

    // artifact
    public static final String STORM_ARTIFACTS_LOCATION_KEY = "stormArtifactsDirectory";
    public static final String STORM_JAR_LOCATION_KEY = "streamlineStormJar";
    public static final String STORM_HOME_DIR = "stormHomeDir";
    public static final String TOPOLOGY_MESSAGE_TIMEOUT_SECS = "topology.message.timeout.secs";
    public static final String TOPOLOGY_MAX_SPOUT_PENDING = "topology.max.spout.pending";
    public static final String STREAMLINE_COMPONENT_CONF_KEY = "streamlineComponent";

    // yaml key constants
    public static final String YAML_KEY_NAME = "name";
    public static final String YAML_KEY_VALUE = "value";
    public static final String YAML_KEY_CATALOG_ROOT_URL = "catalog.root.url";
    public static final String YAML_KEY_LOCAL_PARSER_JAR_PATH = "local.parser.jar.path";
    // TODO: add hbase conf to topology config when processing data sinks
    public static final String YAML_KEY_CONFIG = "config";
    public static final String YAML_KEY_HBASE_CONF = "hbase.conf";
    public static final String YAML_KEY_HBASE_ROOT_DIR = "hbase.root.dir";
    public static final String YAML_KEY_COMPONENTS = "components";
    public static final String YAML_KEY_SPOUTS = "spouts";
    public static final String YAML_KEY_BOLTS = "bolts";
    public static final String YAML_KEY_STREAMS = "streams";
    public static final String YAML_KEY_ID = "id";
    public static final String YAML_KEY_CLASS_NAME = "className";
    public static final String YAML_KEY_PROPERTIES = "properties";
    public static final String YAML_KEY_CONSTRUCTOR_ARGS = "constructorArgs";
    public static final String YAML_KEY_REF = "ref";
    public static final String YAML_KEY_REF_LIST = "reflist";
    public static final String YAML_KEY_ARGS = "args";
    public static final String YAML_KEY_CONFIG_METHODS = "configMethods";
    public static final String YAML_KEY_FROM = "from";
    public static final String YAML_KEY_TO = "to";
    public static final String YAML_KEY_GROUPING = "grouping";
    public static final String YAML_KEY_ALL_GROUPING = "ALL";
    public static final String YAML_KEY_CUSTOM_GROUPING = "CUSTOM";
    public static final String YAML_KEY_DIRECT_GROUPING = "DIRECT";
    public static final String YAML_KEY_SHUFFLE_GROUPING = "SHUFFLE";
    public static final String YAML_KEY_LOCAL_OR_SHUFFLE_GROUPING = "LOCAL_OR_SHUFFLE";
    public static final String YAML_KEY_FIELDS_GROUPING = "FIELDS";
    public static final String YAML_KEY_GLOBAL_GROUPING = "GLOBAL";
    public static final String YAML_KEY_NONE_GROUPING = "NONE";
    public static final String YAML_KEY_TYPE = "type";
    public static final String YAML_PARSED_TUPLES_STREAM = "parsed_tuples_stream";
    public static final String YAML_FAILED_TO_PARSE_TUPLES_STREAM = "failed_to_parse_tuples_stream";
    public static final String YAML_KEY_STREAM_ID = "streamId";
    public final static String YAML_KEY_PARALLELISM = "parallelism";
    public final static String YAML_KEY_CUSTOM_GROUPING_CLASS = "customClass";
    public final static String YAML_KEY_CUSTOM_GROUPING_CLASSNAME = "com.hortonworks.streamline.streams.runtime.storm.grouping.FieldsGroupingAsCustomGrouping";
}
