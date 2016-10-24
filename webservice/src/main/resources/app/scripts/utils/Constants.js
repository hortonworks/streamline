const baseUrl = "/api/v1/catalog/";
const pageSize = 25;
const ItemTypes = {
  ComponentNodes: 'box',
  Nodes: 'node'
};

const Components = {
	Datasource: { value: "Datasource"},
	Datasources: [
                { name:"Device", label:"Device", imgPath: "styles/img/icon-device.png", connectsTo: ["Parser"], hideOnUI:"true" },
                { name:"Kafka", label:"Kafka", imgPath: "styles/img/icon-kafka.png", connectsTo: ["Rule", "Custom", "Normalization", "Split", "Hdfs", "Hbase", "Window"] },
                { name:"Kinesis", label:"Kinesis", imgPath: "styles/img/icon-kinesis.png", connectsTo: ["Parser"], hideOnUI:"true" },
                { name:"Event", label:"Event", imgPath: "styles/img/icon-event.png", connectsTo: ["Parser"], hideOnUI:"true" }
	],
	Processor: { value: "Processor"},
	Processors: [
                { name: "Parser", label:"Parser", imgPath: "styles/img/icon-parser.png", connectsTo: ["Rule", "Custom", "Normalization", "Split", "Hdfs", "Hbase", "Window"], hideOnUI:"true" },
                { name: "Rule", label:"Rule", imgPath: "styles/img/icon-rule.png", connectsTo: ["Rule", "Custom", "Normalization", "Split", "Hdfs", "Hbase", "Notification", "Window"] },
                { name: "Custom", label:"Custom", imgPath: "styles/img/icon-custom.png", connectsTo: ["Rule", "Custom", "Normalization", "Split", "Hdfs", "Hbase", "Notification", "Window"] },
                { name: "Normalization", label:"Normalization", imgPath: "styles/img/icon-normalization.png", connectsTo: ["Rule", "Custom", "Normalization", "Split", "Hdfs", "Hbase", "Notification", "Window"] },
                { name: "Split", label:"Split", imgPath: "styles/img/icon-split.png", connectsTo: ["Stage"] },
                { name: "Stage", label:"Stage", imgPath: "styles/img/icon-stage.png", connectsTo: ["Stage", "Join"] },
                { name: "Join", label:"Join", imgPath: "styles/img/icon-join.png", connectsTo: ["Rule", "Custom", "Normalization", "Hdfs", "Hbase", "Notification", "Window"], hideOnUI:"true" },
                { name: "Window", label:"Aggregate", imgPath: "styles/img/icon-window.png", connectsTo: ["Rule", "Custom", "Normalization", "Split", "Hdfs", "Hbase", "Notification", "Window"] }
	],
	Sink: { value: "Sink"},
	Sinks: [
                { name: "Hdfs", label:"HDFS", imgPath: "styles/img/icon-hdfs.png" },
                { name: "Hbase", label:"HBase", imgPath: "styles/img/icon-hbase.png" },
                { name: "Notification", label:"Notification", imgPath: "styles/img/icon-notification.png" },
                { name: "Hive", label:"Hive", imgPath: "styles/img/icon-hive.png", hideOnUI:"true" },
                { name: "Solr", label:"Solr", imgPath: "styles/img/icon-solr.png", hideOnUI:"true" },
                { name: "Redis", label:"Redis", imgPath: "styles/img/icon-redis.png", hideOnUI:"true" },
                { name: "Elastic Search", label:"Elastic Search", imgPath: "styles/img/icon-elasticsearch.png", hideOnUI:"true" }
	]
};

export {
	baseUrl,
	pageSize,
	ItemTypes,
	Components
};