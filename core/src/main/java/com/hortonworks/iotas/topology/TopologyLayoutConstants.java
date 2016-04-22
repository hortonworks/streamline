package com.hortonworks.iotas.topology;

public final class TopologyLayoutConstants {
    private TopologyLayoutConstants () {}
    // json keys
    public final static String JSON_KEY_TRANSFORMATION_CLASS =
            "transformationClass";
    public final static String JSON_KEY_DATA_SOURCES = "dataSources";
    public final static String JSON_KEY_UINAME = "uiname";
    public final static String JSON_KEY_ID = "id";
    public final static String JSON_KEY_TYPE = "type";
    public final static String JSON_KEY_CONFIG = "config";
    public final static String JSON_KEY_ZK_URL = "zkUrl";
    public final static String JSON_KEY_TOPIC = "topic";
    public final static String JSON_KEY_PROCESSORS = "processors";
    public final static String JSON_KEY_DATA_SINKS = "dataSinks";
    public final static String JSON_KEY_ROOT_DIR = "rootDir";
    public final static String JSON_KEY_TABLE = "table";
    public final static String JSON_KEY_COLUMN_FAMILY = "columnFamily";
    public final static String JSON_KEY_ROW_KEY = "rowKey";
    public final static String JSON_KEY_MAPPER_IMPL =
            "hBaseMapperImplClassName";
    public final static String JSON_KEY_WRITE_TO_WAL = "writeToWAL";
    public final static String JSON_KEY_FS_URL = "fsUrl";
    public final static String JSON_KEY_PATH = "path";
    public final static String JSON_KEY_PREFIX = "prefix";
    public final static String JSON_KEY_EXTENSION = "extension";
    public final static String JSON_KEY_NAME = "name";
    public final static String JSON_KEY_FILE_NAME_FORMAT_IMPL =
            "filenameFormatImpl";
    public final static String JSON_KEY_RECORD_FORMAT_IMPL = "recordFormatImpl";
    public final static String JSON_KEY_SYNC_POLICY_IMPL = "syncPolicyImpl";
    public final static String JSON_KEY_COUNT_POLICY_VALUE = "countPolicyValue";
    public final static String JSON_KEY_ROTATION_POLICY_IMPL =
            "rotationPolicyImpl";
    public final static String JSON_KEY_ROTATION_INTERVAL = "rotationInterval";
    public final static String JSON_KEY_ROTATION_INTERVAL_UNIT =
            "rotationIntervalUnit";
    public final static String JSON_KEY_ROTATION_ACTIONS = "rotationActions";
    public final static String JSON_KEY_LINKS = "links";
    public final static String JSON_KEY_FROM = "from";
    public final static String JSON_KEY_TO = "to";
    public final static String JSON_KEY_CONFIG_KEY = "configKey";
    public final static String JSON_KEY_BROKER_HOSTS_IMPL = "brokerHostsImpl";
    public final static String JSON_KEY_ZK_PATH = "zkPath";
    public final static String JSON_KEY_ZK_ROOT = "zkRoot";
    public final static String JSON_KEY_REFRESH_FREQ_SECS = "refreshFreqSecs";
    public final static String JSON_KEY_SPOUT_CONFIG_ID = "spoutConfigId";
    public final static String JSON_KEY_FETCH_SIZE_BYTES = "fetchSizeBytes";
    public final static String JSON_KEY_SOCKET_TIMEOUT_MS = "socketTimeoutMs";
    public final static String JSON_KEY_FETCH_MAX_WAIT = "fetchMaxWait";
    public final static String JSON_KEY_BUFFER_SIZE_BYTES = "bufferSizeBytes";
    public final static String JSON_KEY_MULTI_SCHEME_IMPL = "multiSchemeImpl";
    public final static String JSON_KEY_IGNORE_ZK_OFFSETS = "ignoreZkOffsets";
    public final static String JSON_KEY_MAX_OFFSET_BEHIND = "maxOffsetBehind";
    public final static String
            JSON_KEY_USE_START_OFFSET_IF_OFFSET_OUT_OF_RANGE = "useStartOffsetTimeIfOffsetOutOfRange";
    public final static String JSON_KEY_METRICS_TIME_BUCKET_SIZE_IN_SECS = "metricsTimeBucketSizeInSecs";
    public final static String JSON_KEY_ZK_SERVERS = "zkServers";
    public final static String JSON_KEY_ZK_PORT = "zkPort";
    public final static String JSON_KEY_STATE_UPDATE_INTERVAL_MS = "stateUpdateIntervalMs";
    public final static String JSON_KEY_RETRY_INITIAL_DELAY_MS = "retryInitialDelayMs";
    public final static String JSON_KEY_RETRY_DELAY_MULTIPLIER = "retryDelayMultiplier";
    public final static String JSON_KEY_RETRY_DELAY_MAX_MS = "retryDelayMaxMs";
    public final static String JSON_KEY_PARSED_TUPLES_STREAM =
            "parsedTuplesStream";
    public final static String JSON_KEY_FAILED_TUPLES_STREAM =
            "failedTuplesStream";
    public final static String JSON_KEY_PARSER_JAR_PATH =
            "parserJarPath";
    public final static String JSON_KEY_PARSER_ID =
            "parserId";
    public final static String JSON_KEY_DATA_SOURCE_ID =
            "dataSourceId";
    public final static String JSON_KEY_STREAM_ID =
            "streamId";
    public final static String JSON_KEY_NOTIFIER_NAME = "notifierName";
    public final static String JSON_KEY_NOTIFIER_JAR_FILENAME = "jarFileName";
    public final static String JSON_KEY_NOTIFIER_CLASSNAME = "className";
    public final static String JSON_KEY_NOTIFIER_PROPERTIES = "properties";
    public final static String JSON_KEY_NOTIFIER_FIELD_VALUES = "fieldValues";
    public final static String JSON_KEY_NOTIFIER_FROM = "from";
    public final static String JSON_KEY_NOTIFIER_TO = "to";
    public final static String JSON_KEY_NOTIFIER_SUBJECT = "subject";
    public final static String JSON_KEY_NOTIFIER_CONTENT_TYPE = "contentType";
    public final static String JSON_KEY_NOTIFIER_BODY = "body";
    public final static String JSON_KEY_NOTIFIER_USERNAME = "username";
    public final static String JSON_KEY_NOTIFIER_PASSWORD = "password";
    public final static String JSON_KEY_NOTIFIER_HOST = "host";
    public final static String JSON_KEY_NOTIFIER_PORT = "port";
    public final static String JSON_KEY_NOTIFIER_SSL = "ssl";
    public final static String JSON_KEY_NOTIFIER_STARTTLS = "starttls";
    public final static String JSON_KEY_NOTIFIER_DEBUG = "debug";
    public final static String JSON_KEY_NOTIFIER_PROTOCOL = "protocol";
    public final static String JSON_KEY_NOTIFIER_AUTH = "auth";
    public final static String JSON_KEY_NOTIFIER_CONFIG_KEY = "hbaseConfigKey";
    public final static String JSON_KEY_GROUPING = "grouping";
    public final static String JSON_KEY_GROUPING_FIELDS = "groupingFields";
    public final static String JSON_KEY_CUSTOM_GROUPING_IMPL = "customGroupingImpl";
    public final static String JSON_KEY_PARALLELISM = "parallelism";
    public final static String JSON_KEY_PARALLELISM_TOOLTIP = "Number of executors";
    public final static String JSON_KEY_RULES_PROCESSOR_CONFIG ="rulesProcessorConfig";
    public final static String JSON_KEY_RULES = "rules";
    public final static String JSON_KEY_RULE_ACTIONS = "actions";
    public final static String JSON_KEY_RULE_DECLARED_OUTPUT = "declaredOutput";

    // normalization processor related constants
    public final static String JSON_KEY_NORMALIZATION_PROCESSOR_CONFIG ="normalizationProcessorConfig";

    public final static String JSON_KEY_INPUT_SCHEMA = "inputSchema";
    public final static String JSON_KEY_OUTPUT_STREAMS_SCHEMA = "outputStreamToSchema";
    public final static String JSON_KEY_CUSTOM_PROCESSOR_DESCRIPTION = "description";
    public final static String JSON_KEY_CUSTOM_PROCESSOR_IMPL = "customProcessorImpl";
    public final static String JSON_KEY_CUSTOM_PROCESSOR_JAR_FILENAME = "jarFileName";
    public final static String JSON_KEY_CUSTOM_PROCESSOR_IMAGE_FILENAME = "imageFileName";
    public final static String JSON_KEY_CUSTOM_PROCESSOR_SUB_TYPE = "CUSTOM";
    public final static String JSON_KEY_LOCAL_JAR_PATH = "localJarPath";
    public final static String JSON_KEY_LOCAL_JAR_PATH_TOOLTIP = "Local path on worker node to download jar.";
    // Custom processor's own config fields will be uploaded in IoTaS topology components with a namespace to avoid collision with the CustomProcessorBolt's
    // config fields. The below constant represents that namespace and the other is regular expression is to remove the namespace when forwarding the config
    // values to the custom processor implementation
    public final static String JSON_KEY_CUSTOM_PROCESSOR_PREFIX = "config.";
    public final static String JSON_KEY_CUSTOM_PROCESSOR_PREFIX_REGEX = "config\\.";

    // validation error messages
    public final static String ERR_MSG_UINAME_DUP = "Uiname %s is already " +
            "used by other component.";
    public final static String ERR_MSG_LINK_FROM = "Link from property %s " +
            "has to be a data source component or a processor component";
    public final static String ERR_MSG_LINK_TO = "Link to property %s " +
            "has to be a data sink component or a processor component.";
    public final static String ERR_MSG_LOOP = "Link from property %s " +
            "cannot be same as link to property %s";
    public final static String ERR_MSG_DISCONNETED_DATA_SOURCE = "Data Source" +
            " %s is not linked to any component.";
    public final static String ERR_MSG_DISCONNETED_DATA_SINK = "Data Sink " +
            "%s is not linked to any component.";
    public static final String ERR_MSG_DISCONNETED_PROCESSOR_IN = "Processor " +
            "%s does not take an input.";
    public static final String ERR_MSG_DISCONNETED_PROCESSOR_OUT = "Processor" +
            " %s does not connect to an output.";
    public static final String ERR_MSG_MISSING_INVALID_CONFIG =
            "Missing or invalid config property %s.";
    public static final String ERR_MSG_NO_PARSER_PROCESSOR = "No parser " +
            "processor in topology.";
    public static final String ERR_MSG_INVALID_STREAM_ID = "Invalid " +
            "stream id for link %s ";
    public static final String ERR_MSG_INVALID_LINK_TO_PARSER = "Link %s to a" +
            " parser processor can only be from a data source ";
    public static final String ERR_MSG_INVALID_LINK_TO_PROCESSOR = "Link %s to a" +
            " rule/custom processor can not be from a data source ";
    public static final String ERR_MSG_INVALID_GROUPING_FIELDS = "Link %s  " +
            "has invalid fields for grouping ";
    public static final String ERR_MSG_CP_IMPL_INSTANTIATION = "Error instantiating custom processor implementation class %s ";
    public static final String ERR_MSG_CP_CONFIG_EXCEPTION = "Custom processor %s threw exception while validating the custom config fields.";

    public static final String STORM_STREAMING_ENGINE = "STORM";

    // artifact
    public static final String STORM_ARTIFACTS_LOCATION_KEY =
            "stormArtifactsDirectory";
    public static final String STORM_JAR_LOCATION_KEY = "iotasStormJar";
    public static final String STORM_HOME_DIR = "stormHomeDir";
    // yaml key constants
    public static final String YAML_KEY_NAME = "name";
    public static final String YAML_KEY_VALUE = "value";
    public static final String YAML_KEY_CATALOG_ROOT_URL = "catalog.root.url";
    public static final String YAML_KEY_LOCAL_PARSER_JAR_PATH = "local" +
                    ".parser.jar.path";
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
    public static final String YAML_KEY_ARGS = "args";
    public static final String YAML_KEY_CONFIG_METHODS = "configMethods";
    public static final String YAML_KEY_FROM = "from";
    public static final String YAML_KEY_TO = "to";
    public static final String YAML_KEY_GROUPING = "grouping";
    public static final String YAML_KEY_ALL_GROUPING = "ALL";
    public static final String YAML_KEY_CUSTOM_GROUPING = "CUSTOM";
    public static final String YAML_KEY_DIRECT_GROUPING = "DIRECT";
    public static final String YAML_KEY_SHUFFLE_GROUPING = "SHUFFLE";
    public static final String YAML_KEY_LOCAL_OR_SHUFFLE_GROUPING =
            "LOCAL_OR_SHUFFLE";
    public static final String YAML_KEY_FIELDS_GROUPING = "FIELDS";
    public static final String YAML_KEY_GLOBAL_GROUPING = "GLOBAL";
    public static final String YAML_KEY_NONE_GROUPING = "NONE";
    public static final String YAML_KEY_TYPE = "type";
    public static final String YAML_PARSED_TUPLES_STREAM =
            "parsed_tuples_stream";
    public static final String YAML_FAILED_TO_PARSE_TUPLES_STREAM =
            "failed_to_parse_tuples_stream";
    public static final String YAML_KEY_STREAM_ID = "streamId";
    public final static String YAML_KEY_PARALLELISM = "parallelism";

}
