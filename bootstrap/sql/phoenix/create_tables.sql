CREATE TABLE IF NOT EXISTS file ("id" BIGINT NOT NULL, "name" VARCHAR(256) ,"version" BIGINT, "description" VARCHAR ,"storedFileName" VARCHAR , "timestamp"  BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topology ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "name" VARCHAR (256), "description" VARCHAR, "namespaceId" BIGINT, "config" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS topology_component_bundle("id" BIGINT NOT NULL, "name" VARCHAR(256), "type" VARCHAR, "subType" VARCHAR, "streamingEngine" VARCHAR, "topologyComponentUISpecification" VARCHAR, "fieldHintProviderClass" VARCHAR, "transformationClass" VARCHAR, "timestamp"  BIGINT, "bundleJar" VARCHAR, "builtin" CHAR(4), "mavenDeps" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topology_editor_metadata ("topologyId" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "data" VARCHAR, "timestamp"  BIGINT, CONSTRAINT pk PRIMARY KEY ("topologyId", "versionId"))
CREATE TABLE IF NOT EXISTS topology_editor_toolbar ("userId" BIGINT NOT NULL, "data" VARCHAR, "timestamp"  BIGINT, CONSTRAINT pk PRIMARY KEY ("userId"))
CREATE TABLE IF NOT EXISTS tag ("id" BIGINT NOT NULL, "name" VARCHAR(256), "description" VARCHAR(256), "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS tag_storable_mapping ("tagId" BIGINT NOT NULL, "storableNamespace" VARCHAR(32) NOT NULL, "storableId" BIGINT NOT NULL, CONSTRAINT pk PRIMARY KEY ("tagId", "storableNamespace", "storableId"))
CREATE TABLE IF NOT EXISTS notifier ("id" BIGINT  NOT NULL, "name" VARCHAR, "description" VARCHAR, "jarFileName" VARCHAR, "className" VARCHAR, "timestamp"  BIGINT, "properties" VARCHAR, "fieldValues" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topology_stream ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "streamId" VARCHAR(256), "description" VARCHAR, "fieldsData" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS topology_component ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "topologyComponentBundleId" BIGINT, "name" VARCHAR, "description" VARCHAR, "configData" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS topology_source ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "topologyComponentBundleId" BIGINT, "name" VARCHAR, "description" VARCHAR, "configData" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS topology_source_stream_mapping ("sourceId" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "streamId" BIGINT NOT NULL, CONSTRAINT pk PRIMARY KEY ("sourceId", "versionId", "streamId"))
CREATE TABLE IF NOT EXISTS topology_sink ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "topologyComponentBundleId" BIGINT, "name" VARCHAR, "description" VARCHAR, "configData" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS topology_processor ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "topologyComponentBundleId" BIGINT, "name" VARCHAR, "description" VARCHAR, "configData" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS topology_processor_stream_mapping ("processorId" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "streamId" BIGINT NOT NULL, CONSTRAINT pk PRIMARY KEY ("processorId", "versionId", "streamId"))
CREATE TABLE IF NOT EXISTS topology_edge ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "fromId" BIGINT, "toId" BIGINT, "streamGroupingsData" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS topology_version ("id" BIGINT NOT NULL, "topologyId" BIGINT, "name" VARCHAR(256), "description" VARCHAR(256), "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topology_rule ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "name" VARCHAR, "description" VARCHAR, "streams" VARCHAR, "outputStreams" VARCHAR, "condition" VARCHAR, "sql" VARCHAR, "parsedRuleStr" VARCHAR, "projections" VARCHAR, "window" VARCHAR, "actions" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS topology_branchrule ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "name" VARCHAR, "description" VARCHAR, "stream" VARCHAR, "outputStreams" VARCHAR, "condition" VARCHAR, "parsedRuleStr" VARCHAR, "actions" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS topology_window ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "name" VARCHAR, "description" VARCHAR, "streams" VARCHAR, "outputStreams" VARCHAR, "condition" VARCHAR, "parsedRuleStr" VARCHAR, "window" VARCHAR, "actions" VARCHAR, "projections" VARCHAR, "groupbykeys" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS udf ("id" BIGINT NOT NULL, "name" VARCHAR, "displayName" VARCHAR, "description" VARCHAR, "type" VARCHAR, "className" VARCHAR, "jarStoragePath" VARCHAR, "digest" VARCHAR, "argTypes" VARCHAR, "returnType" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS cluster ("id" BIGINT NOT NULL, "name" VARCHAR, "ambariImportUrl" VARCHAR, "description" VARCHAR, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS service ("id" BIGINT NOT NULL, "clusterId" BIGINT, "name" VARCHAR, "description" VARCHAR, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS service_configuration ("id" BIGINT NOT NULL, "serviceId" BIGINT, "name" VARCHAR, "configuration" VARCHAR, "description" VARCHAR, "filename" VARCHAR, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS component ("id" BIGINT NOT NULL, "serviceId" BIGINT, "name" VARCHAR, "hosts" VARCHAR, "protocol" VARCHAR, "port" INTEGER, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS namespace ("id" BIGINT NOT NULL, "name" VARCHAR, "streamingEngine" VARCHAR, "timeSeriesDB" VARCHAR, "description" VARCHAR, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS namespace_service_cluster_mapping ("namespaceId" BIGINT NOT NULL, "serviceName" VARCHAR NOT NULL, "clusterId" BIGINT NOT NULL, CONSTRAINT pk PRIMARY KEY ("namespaceId", "serviceName", "clusterId"))
CREATE TABLE IF NOT EXISTS dashboard ("id" BIGINT NOT NULL, "name" VARCHAR, "description" VARCHAR, "data" VARCHAR, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS widget ("id" BIGINT NOT NULL, "dashboardId" BIGINT NOT NULL, "name" VARCHAR, "description" VARCHAR, "type" VARCHAR, "data" VARCHAR, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id", "dashboardId"))
CREATE TABLE IF NOT EXISTS datasource ("id" BIGINT NOT NULL, "dashboardId" BIGINT NOT NULL, "name" VARCHAR, "description" VARCHAR, "type" VARCHAR, "url" VARCHAR, "data" VARCHAR, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id", "dashboardId"))
CREATE TABLE IF NOT EXISTS widget_datasource_mapping ("widgetId" BIGINT NOT NULL, "datasourceId" BIGINT NOT NULL, CONSTRAINT pk PRIMARY KEY ("widgetId", "datasourceId"))
CREATE TABLE IF NOT EXISTS ml_model ("id" BIGINT NOT NULL, "name" VARCHAR(256) NOT NULL, "pmml" VARCHAR, "uploadedFileName" VARCHAR(256), "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id", "name"))
CREATE TABLE IF NOT EXISTS topology_state ("topologyId" BIGINT NOT NULL, "name" VARCHAR(255) NOT NULL, "description" VARCHAR(255) NOT NULL, CONSTRAINT pk PRIMARY KEY ("topologyId"))
CREATE TABLE IF NOT EXISTS service_bundle ("id" BIGINT NOT NULL, "name" VARCHAR(256), "serviceUISpecification" VARCHAR, "registerClass" VARCHAR, "timestamp"  BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS acl_entry ("id" BIGINT NOT NULL, "objectId" BIGINT NOT NULL, "objectNamespace" VARCHAR(256) NOT NULL, "sidId" BIGINT NOT NULL, "sidType" VARCHAR(256) NOT NULL, "permissions" VARCHAR(256) NOT NULL, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS role ("id" BIGINT NOT NULL, "name" VARCHAR(256) NOT NULL, "description" VARCHAR, "system" BOOLEAN NOT NULL, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS role_hierarchy ("parentId" BIGINT NOT NULL, "childId" BIGINT NOT NULL, CONSTRAINT pk PRIMARY KEY ("parentId", "childId"))
CREATE TABLE IF NOT EXISTS user_entry ("id" BIGINT NOT NULL, "name" VARCHAR(256) NOT NULL, "email" VARCHAR(256) NOT NULL, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS user_role ("userId" BIGINT NOT NULL, "roleId" BIGINT NOT NULL, CONSTRAINT pk PRIMARY KEY ("userId", "roleId"))
CREATE TABLE IF NOT EXISTS topology_test_run_case ("id" BIGINT NOT NULL, "name" VARCHAR(256) NOT NULL, "topologyId" BIGINT NOT NULL, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topology_test_run_case_source ("id" BIGINT NOT NULL, "testCaseId" BIGINT NOT NULL, "sourceId" BIGINT NOT NULL, "records" VARCHAR NOT NULL, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topology_test_run_case_sink ("id" BIGINT NOT NULL, "testCaseId" BIGINT NOT NULL, "sinkId" BIGINT NOT NULL, "records" VARCHAR NOT NULL, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topology_test_run_histories ("id" BIGINT NOT NULL, "topologyId" BIGINT NOT NULL, "versionId" BIGINT, "testRecords" TEXT NOT NULL, "finished" CHAR(5) NOT NULL, "success" CHAR(5) NOT NULL, "expectedOutputRecords" VARCHAR, "actualOutputRecords" VARCHAR, "matched" CHAR(5), "startTime" BIGINT, "finishTime" BIGINT, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS sequence_table ("id" VARCHAR, "file" BIGINT, "topology_version" BIGINT, "topology" BIGINT, "topology_component_bundle" BIGINT,"topology_component" BIGINT, "tag" BIGINT,  "topology_stream" BIGINT, "notifier" BIGINT, "topology_source" BIGINT, "topology_sink" BIGINT, "topology_processor" BIGINT, "topology_edge" BIGINT,"topology_rule" BIGINT, "topology_window" BIGINT, "udf" BIGINT, "cluster" BIGINT, "service" BIGINT, "service_configuration" BIGINT,"topology_branchrule" BIGINT, "component" BIGINT, "dashboard" BIGINT, "widget" BIGINT, "datasource" BIGINT, "namespace" BIGINT, "ml_model" BIGINT, "topology_state" BIGINT, "service_bundle" BIGINT, "acl_entry" BIGINT, "role" BIGINT, "role_hierarchy" BIGINT, "user_entry" BIGINT, "user_role" BIGINT, "topology_editor_toolbar" BIGINT, "topology_test_run_case" BIGINT, "topology_test_run_case_source" BIGINT, "topology_test_run_case_sink" BIGINT, "topology_test_run_histories" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))

CREATE SEQUENCE IF NOT EXISTS topology_version_sequence
CREATE SEQUENCE IF NOT EXISTS topology_sequence
CREATE SEQUENCE IF NOT EXISTS topology_component_bundle_sequence
CREATE SEQUENCE IF NOT EXISTS tag_sequence
CREATE SEQUENCE IF NOT EXISTS file_sequence
CREATE SEQUENCE IF NOT EXISTS topology_stream_sequence
CREATE SEQUENCE IF NOT EXISTS notifier_sequence
CREATE SEQUENCE IF NOT EXISTS topology_component_sequence
CREATE SEQUENCE IF NOT EXISTS topology_source_sequence
CREATE SEQUENCE IF NOT EXISTS topology_sink_sequence
CREATE SEQUENCE IF NOT EXISTS topology_processor_sequence
CREATE SEQUENCE IF NOT EXISTS topology_edge_sequence
CREATE SEQUENCE IF NOT EXISTS topology_rule_sequence
CREATE SEQUENCE IF NOT EXISTS topology_branchrule_sequence
CREATE SEQUENCE IF NOT EXISTS topology_window_sequence
CREATE SEQUENCE IF NOT EXISTS udf_sequence
CREATE SEQUENCE IF NOT EXISTS cluster_sequence
CREATE SEQUENCE IF NOT EXISTS service_sequence
CREATE SEQUENCE IF NOT EXISTS service_configuration_sequence
CREATE SEQUENCE IF NOT EXISTS component_sequence
CREATE SEQUENCE IF NOT EXISTS namespace_sequence
CREATE SEQUENCE IF NOT EXISTS dashboard_sequence
CREATE SEQUENCE IF NOT EXISTS widget_sequence
CREATE SEQUENCE IF NOT EXISTS datasource_sequence
CREATE SEQUENCE IF NOT EXISTS ml_model_sequence
CREATE SEQUENCE IF NOT EXISTS topology_state_sequence
CREATE SEQUENCE IF NOT EXISTS service_bundle_sequence
CREATE SEQUENCE IF NOT EXISTS acl_entry_sequence
CREATE SEQUENCE IF NOT EXISTS role_sequence
CREATE SEQUENCE IF NOT EXISTS role_hierarchy_sequence
CREATE SEQUENCE IF NOT EXISTS user_entry_sequence
CREATE SEQUENCE IF NOT EXISTS user_role_sequence
CREATE SEQUENCE IF NOT EXISTS topology_editor_toolbar_sequence
CREATE SEQUENCE IF NOT EXISTS topology_test_run_case_sequence
CREATE SEQUENCE IF NOT EXISTS topology_test_run_case_source_sequence
CREATE SEQUENCE IF NOT EXISTS topology_test_run_case_sink_sequence
CREATE SEQUENCE IF NOT EXISTS topology_test_run_histories_sequence
