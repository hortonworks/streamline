CREATE TABLE IF NOT EXISTS parser_info ("id" BIGINT NOT NULL, "name" VARCHAR(256) ,"version" BIGINT, "className" VARCHAR , "jarStoragePath" VARCHAR ,"parserSchema" VARCHAR, "timestamp"  BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS files ("id" BIGINT NOT NULL, "name" VARCHAR(256) ,"version" BIGINT, "description" VARCHAR ,"storedFileName" VARCHAR , "timestamp"  BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topologies ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "name" VARCHAR (256), "description" VARCHAR, "namespaceId" BIGINT, "config" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS topology_component_bundles("id" BIGINT NOT NULL, "name" VARCHAR(256), "type" VARCHAR, "subType" VARCHAR, "streamingEngine" VARCHAR, "topologyComponentUISpecification" VARCHAR, "schemaClass" VARCHAR, "transformationClass" VARCHAR, "timestamp"  BIGINT, "bundleJar" VARCHAR, "builtin" CHAR(4), "mavenDeps" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topology_editor_metadata ("topologyId" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "data" VARCHAR, "timestamp"  BIGINT, CONSTRAINT pk PRIMARY KEY ("topologyId", "versionId"))
CREATE TABLE IF NOT EXISTS tag ("id" BIGINT NOT NULL, "name" VARCHAR(256), "description" VARCHAR(256), "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS tag_storable_mapping ("tagId" BIGINT NOT NULL, "storableNamespace" VARCHAR(32) NOT NULL, "storableId" BIGINT NOT NULL, CONSTRAINT pk PRIMARY KEY ("tagId", "storableNamespace", "storableId"))
CREATE TABLE IF NOT EXISTS notifierinfos ("id" BIGINT  NOT NULL, "name" VARCHAR, "description" VARCHAR, "jarFileName" VARCHAR, "className" VARCHAR, "timestamp"  BIGINT, "properties" VARCHAR, "fieldValues" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS streaminfo ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "streamId" VARCHAR(256), "description" VARCHAR, "fieldsData" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS topology_components ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "topologyComponentBundleId" BIGINT, "name" VARCHAR, "description" VARCHAR, "configData" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS topology_sources ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "topologyComponentBundleId" BIGINT, "name" VARCHAR, "description" VARCHAR, "configData" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS topology_source_stream_mapping ("sourceId" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "streamId" BIGINT NOT NULL, CONSTRAINT pk PRIMARY KEY ("sourceId", "versionId", "streamId"))
CREATE TABLE IF NOT EXISTS topology_sinks ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "topologyComponentBundleId" BIGINT, "name" VARCHAR, "description" VARCHAR, "configData" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS topology_processors ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "topologyComponentBundleId" BIGINT, "name" VARCHAR, "description" VARCHAR, "configData" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS topology_processor_stream_mapping ("processorId" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "streamId" BIGINT NOT NULL, CONSTRAINT pk PRIMARY KEY ("processorId", "versionId", "streamId"))
CREATE TABLE IF NOT EXISTS topology_edges ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "fromId" BIGINT, "toId" BIGINT, "streamGroupingsData" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS topology_versioninfos ("id" BIGINT NOT NULL, "topologyId" BIGINT, "name" VARCHAR(256), "description" VARCHAR(256), "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS ruleinfos ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "name" VARCHAR, "description" VARCHAR, "streams" VARCHAR, "condition" VARCHAR, "sql" VARCHAR, "parsedRuleStr" VARCHAR, "projections" VARCHAR, "window" VARCHAR, "actions" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS branchruleinfos ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "name" VARCHAR, "description" VARCHAR, "stream" VARCHAR, "condition" VARCHAR, "parsedRuleStr" VARCHAR, "actions" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS windowinfos ("id" BIGINT NOT NULL, "versionId" BIGINT NOT NULL, "topologyId" BIGINT, "name" VARCHAR, "description" VARCHAR, "streams" VARCHAR, "condition" VARCHAR, "parsedRuleStr" VARCHAR, "window" VARCHAR, "actions" VARCHAR, "projections" VARCHAR, "groupbykeys" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id", "versionId"))
CREATE TABLE IF NOT EXISTS udfs ("id" BIGINT NOT NULL, "name" VARCHAR, "displayName" VARCHAR, "description" VARCHAR, "type" VARCHAR, "className" VARCHAR, "jarStoragePath" VARCHAR, "digest" VARCHAR, "argTypes" VARCHAR, "returnType" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS clusters ("id" BIGINT NOT NULL, "name" VARCHAR, "description" VARCHAR, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS services ("id" BIGINT NOT NULL, "clusterId" BIGINT, "name" VARCHAR, "description" VARCHAR, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS service_configurations ("id" BIGINT NOT NULL, "serviceId" BIGINT, "name" VARCHAR, "configuration" VARCHAR, "description" VARCHAR, "filename" VARCHAR, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS components ("id" BIGINT NOT NULL, "serviceId" BIGINT, "name" VARCHAR, "hosts" VARCHAR, "protocol" VARCHAR, "port" INTEGER, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS namespaces ("id" BIGINT NOT NULL, "name" VARCHAR, "streamingEngine" VARCHAR, "timeSeriesDB" VARCHAR, "description" VARCHAR, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS namespace_service_cluster_mapping ("namespaceId" BIGINT NOT NULL, "serviceName" VARCHAR NOT NULL, "clusterId" BIGINT NOT NULL, CONSTRAINT pk PRIMARY KEY ("namespaceId", "serviceName", "clusterId"))
CREATE TABLE IF NOT EXISTS dashboard ("id" BIGINT NOT NULL, "name" VARCHAR, "description" VARCHAR, "data" VARCHAR, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS widget ("id" BIGINT NOT NULL, "dashboardId" BIGINT NOT NULL, "name" VARCHAR, "description" VARCHAR, "type" VARCHAR, "data" VARCHAR, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id", "dashboardId"))
CREATE TABLE IF NOT EXISTS datasource ("id" BIGINT NOT NULL, "dashboardId" BIGINT NOT NULL, "name" VARCHAR, "description" VARCHAR, "type" VARCHAR, "url" VARCHAR, "data" VARCHAR, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id", "dashboardId"))
CREATE TABLE IF NOT EXISTS widget_datasource_mapping ("widgetId" BIGINT NOT NULL, "datasourceId" BIGINT NOT NULL, CONSTRAINT pk PRIMARY KEY ("widgetId", "datasourceId"))
CREATE TABLE IF NOT EXISTS sequence_table ("id" VARCHAR, "parser_info" BIGINT, "files" BIGINT, "topology_versioninfos" BIGINT, "topologies" BIGINT, "topology_component_bundles" BIGINT,"topology_components" BIGINT, "tag" BIGINT,  "streaminfo" BIGINT, "notifierinfos" BIGINT, "topology_sources" BIGINT, "topology_sinks" BIGINT, "topology_processors" BIGINT, "topology_edges" BIGINT,"ruleinfos" BIGINT, "windowinfos" BIGINT, "udfs" BIGINT, "clusters" BIGINT, "services" BIGINT, "service_configurations" BIGINT,"branchruleinfos" BIGINT, "components" BIGINT,"namespaces" BIGINT CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS ml_models ("id" BIGINT NOT NULL, "name" VARCHAR(256) NOT NULL, "pmml" VARCHAR, "uploadedFileName" VARCHAR(256) NOT NULL, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))

CREATE SEQUENCE IF NOT EXISTS parser_info_sequence
CREATE SEQUENCE IF NOT EXISTS topology_versioninfos_sequence
CREATE SEQUENCE IF NOT EXISTS topologies_sequence
CREATE SEQUENCE IF NOT EXISTS topology_component_bundles_sequence
CREATE SEQUENCE IF NOT EXISTS tag_sequence
CREATE SEQUENCE IF NOT EXISTS files_sequence
CREATE SEQUENCE IF NOT EXISTS streaminfo_sequence
CREATE SEQUENCE IF NOT EXISTS notifierinfos_sequence
CREATE SEQUENCE IF NOT EXISTS topology_components_sequence
CREATE SEQUENCE IF NOT EXISTS topology_sources_sequence
CREATE SEQUENCE IF NOT EXISTS topology_sinks_sequence
CREATE SEQUENCE IF NOT EXISTS topology_processors_sequence
CREATE SEQUENCE IF NOT EXISTS topology_edges_sequence
CREATE SEQUENCE IF NOT EXISTS ruleinfos_sequence
CREATE SEQUENCE IF NOT EXISTS branchruleinfos_sequence
CREATE SEQUENCE IF NOT EXISTS windowinfos_sequence
CREATE SEQUENCE IF NOT EXISTS udfs_sequence
CREATE SEQUENCE IF NOT EXISTS clusters_sequence
CREATE SEQUENCE IF NOT EXISTS services_sequence
CREATE SEQUENCE IF NOT EXISTS service_configurations_sequence
CREATE SEQUENCE IF NOT EXISTS components_sequence
CREATE SEQUENCE IF NOT EXISTS namespaces_sequence
CREATE SEQUENCE IF NOT EXISTS dashboard_sequence
CREATE SEQUENCE IF NOT EXISTS widget_sequence
CREATE SEQUENCE IF NOT EXISTS datasource_sequence
CREATE SEQUENCE IF NOT EXISTS ml_models_sequence
