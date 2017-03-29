package com.hortonworks.streamline.streams.cluster;

public final class Constants {
    private Constants() {
    }

    public static class Zookeeper {
        public static final String SERVICE_NAME = "ZOOKEEPER";
    }

    public static class Storm {
        public static final String SERVICE_NAME = "STORM";
    }

    public static class Kafka {
        public static final String SERVICE_NAME = "KAFKA";
        public static final String PROPERTY_KEY_ZOOKEEPER_CONNECT = "zookeeper.connect";
    }

    public static class HDFS {
        public static final String SERVICE_NAME = "HDFS";
        public static final String PROPERTY_KEY_DEFAULT_FS = "fs.defaultFS";
    }

    public static class HBase {
        public static final String SERVICE_NAME = "HBASE";
        public static final String PROPERTY_KEY_HBASE_ZOOKEEPER_QUORUM = "hbase.zookeeper.quorum";
    }

    public static class Hive {
        public static final String SERVICE_NAME = "HIVE";
        public static final String PROPERTY_KEY_HIVE_ZOOKEEPER_QUORUM = "hive.zookeeper.quorum";
    }

    public static class EventHubs {
        public static final String SERVICE_NAME = "EVENTHUBS";
    }

    public static class Druid {
        public static final String SERVICE_NAME = "DRUID";
        public static final String CONF_TYPE_COMMON_RUNTIME = "common.runtime"; // common.runtime.properties
        public static final String PROPERTY_KEY_ZK_SERVICE_HOSTS = "druid.zk.service.host";
        public static final String PROPERTY_KEY_INDEXING_SERVICE_NAME = "druid.selectors.indexing.serviceName";
        public static final String PROPERTY_KEY_DISCOVERY_CURATOR_PATH = "druid.discovery.curator.path";
    }
}
