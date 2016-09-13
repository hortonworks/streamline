CREATE TABLE IF NOT EXISTS datasources ("id" BIGINT, "name" VARCHAR, "description" VARCHAR, "tags" VARCHAR, "timestamp"  BIGINT, "type" VARCHAR ,"typeConfig" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS devices ("make" VARCHAR , "model" VARCHAR, "dataSourceId" BIGINT , CONSTRAINT pk PRIMARY KEY ("dataSourceId"))
CREATE TABLE IF NOT EXISTS parser_info ("id" BIGINT NOT NULL, "name" VARCHAR(256) ,"version" BIGINT, "className" VARCHAR , "jarStoragePath" VARCHAR ,"parserSchema" VARCHAR, "timestamp"  BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS files ("id" BIGINT NOT NULL, "name" VARCHAR(256) ,"version" BIGINT, "auxiliaryInfo" VARCHAR ,"storedFileName" VARCHAR , "timestamp"  BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS datafeeds ("id" BIGINT NOT NULL, "dataSourceId" BIGINT , "name" VARCHAR(256) , "description" VARCHAR, "tags" VARCHAR, "parserId" BIGINT , "type" VARCHAR , CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topologies ("id" BIGINT NOT NULL, "name" VARCHAR (256), "config" VARCHAR, "timestamp"  BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topology_component_definitions ("id" BIGINT NOT NULL, "name" VARCHAR(256), "type" VARCHAR, "subType" VARCHAR, "streamingEngine" VARCHAR, "config" VARCHAR, "schemaClass" VARCHAR, "transformationClass" VARCHAR, "timestamp"  BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topology_editor_metadata ("topologyId" BIGINT NOT NULL, "data" VARCHAR, "timestamp"  BIGINT, CONSTRAINT pk PRIMARY KEY ("topologyId"))
CREATE TABLE IF NOT EXISTS tag ("id" BIGINT NOT NULL, "name" VARCHAR(256), "description" VARCHAR(256), "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS tag_storable_mapping ("tagId" BIGINT NOT NULL, "storableNamespace" VARCHAR(32) NOT NULL, "storableId" BIGINT NOT NULL, CONSTRAINT pk PRIMARY KEY ("tagId", "storableNamespace", "storableId"))
CREATE TABLE IF NOT EXISTS notifierinfos ("id" BIGINT  NOT NULL, "name" VARCHAR, "jarFileName" VARCHAR, "className" VARCHAR, "timestamp"  BIGINT, "properties" VARCHAR, "fieldValues" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS streaminfo ("id" BIGINT NOT NULL, "topologyId" BIGINT, "streamId" VARCHAR(256), "fieldsData" VARCHAR, "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topology_components ("id" BIGINT NOT NULL, "topologyId" BIGINT, "name" VARCHAR, "type" VARCHAR, "configData" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topology_sources ("id" BIGINT NOT NULL, "topologyId" BIGINT, "name" VARCHAR, "type" VARCHAR, "configData" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topology_source_stream_mapping ("sourceId" BIGINT NOT NULL, "streamId" BIGINT NOT NULL, CONSTRAINT pk PRIMARY KEY ("sourceId", "streamId"))
CREATE TABLE IF NOT EXISTS topology_sinks ("id" BIGINT NOT NULL, "topologyId" BIGINT, "name" VARCHAR, "type" VARCHAR, "configData" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topology_processors ("id" BIGINT NOT NULL, "topologyId" BIGINT, "name" VARCHAR, "type" VARCHAR, "configData" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topology_processor_stream_mapping ("processorId" BIGINT NOT NULL, "streamId" BIGINT NOT NULL, CONSTRAINT pk PRIMARY KEY ("processorId", "streamId"))
CREATE TABLE IF NOT EXISTS topology_edges ("id" BIGINT NOT NULL, "topologyId" BIGINT, "fromId" BIGINT, "toId" BIGINT, "streamGroupingsData" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS ruleinfos ("id" BIGINT NOT NULL, "topologyId" BIGINT, "name" VARCHAR, "description" VARCHAR, "sqlStr" VARCHAR, "parsedRuleStr" VARCHAR, "window" VARCHAR, "actions" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS udfs ("id" BIGINT NOT NULL, "name" VARCHAR, "description" VARCHAR, "type" VARCHAR, "className" VARCHAR, "jarStoragePath" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS sequence_table ("id" VARCHAR, "datasources" BIGINT, "datafeeds" BIGINT, "parser_info" BIGINT, "files" BIGINT, "topologies" BIGINT, "topology_component_definitions" BIGINT, "topology_components" BIGINT, "tag" BIGINT,  "streaminfo" BIGINT, "notifierinfos" BIGINT, "topology_sources" BIGINT, "topology_sinks" BIGINT, "topology_processors" BIGINT, "topology_edges" BIGINT, "ruleinfos" BIGINT, "udfs" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE SEQUENCE IF NOT EXISTS datasources_sequence
CREATE SEQUENCE IF NOT EXISTS datafeeds_sequence
CREATE SEQUENCE IF NOT EXISTS parser_info_sequence
CREATE SEQUENCE IF NOT EXISTS topologies_sequence
CREATE SEQUENCE IF NOT EXISTS topology_component_definitions_sequence
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
CREATE SEQUENCE IF NOT EXISTS udfs_sequence
