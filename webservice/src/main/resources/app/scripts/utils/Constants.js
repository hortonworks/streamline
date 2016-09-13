const baseUrl = "/api/v1/catalog/";
const pageSize = 25;
const ItemTypes = {
  ComponentNodes: 'box',
  Nodes: 'node'
};

const Components = {
	Datasource: { value: "Datasource"},
	Datasources: [
		{ name:"Device", imgPath: "styles/img/device.png", connectsTo: ["Parser"] },
		{ name:"Kafka", imgPath: "styles/img/kafka.png", connectsTo: ["Parser"], hideOnUI:"true" },
		{ name:"Kinesis", imgPath: "styles/img/kinesis.png", connectsTo: ["Parser"], hideOnUI:"true" },
		{ name:"Event", imgPath: "styles/img/event.png", connectsTo: ["Parser"], hideOnUI:"true" }
	],
	Processor: { value: "Processor"},
	Processors: [
		{ name: "Parser", imgPath: "styles/img/parser.png", connectsTo: ["Rule", "Custom", "Normalization", "Split", "Hdfs", "Hbase"], hideOnUI:"true" },
		{ name: "Rule", imgPath: "styles/img/rule.png", connectsTo: ["Rule", "Custom", "Normalization", "Split", "Hdfs", "Hbase", "Notification"] },
		{ name: "Custom", imgPath: "styles/img/custom.png", connectsTo: ["Rule", "Custom", "Normalization", "Split", "Hdfs", "Hbase", "Notification"] },
		{ name: "Normalization", imgPath: "styles/img/normalization.png", connectsTo: ["Rule", "Custom", "Normalization", "Split", "Hdfs", "Hbase", "Notification"] },
		{ name: "Split", imgPath: "styles/img/split.png", connectsTo: ["Stage"] },
		{ name: "Stage", imgPath: "styles/img/stage.png", connectsTo: ["Stage", "Join"] },
		{ name: "Join", imgPath: "styles/img/join.png", connectsTo: ["Rule", "Custom", "Normalization", "Hdfs", "Hbase", "Notification"], hideOnUI:"true" }
	],
	Sink: { value: "Sink"},
	Sinks: [
		{ name: "Hdfs", imgPath: "styles/img/hdfs.png" },
		{ name: "Hbase", imgPath: "styles/img/hbase.png" },
		{ name: "Notification", imgPath: "styles/img/notification.png" },
		{ name: "Hive", imgPath: "styles/img/hive.png", hideOnUI:"true" },
		{ name: "Solr", imgPath: "styles/img/solr.png", hideOnUI:"true" },
		{ name: "Redis", imgPath: "styles/img/redis.png", hideOnUI:"true" },
		{ name: "Elastic Search", imgPath: "styles/img/elasticsearch.png", hideOnUI:"true" }
	]
};

export {
	baseUrl,
	ItemTypes,
	Components
};