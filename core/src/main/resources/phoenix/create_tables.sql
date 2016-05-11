CREATE TABLE IF NOT EXISTS datasources ("id" BIGINT, "name" VARCHAR, "description" VARCHAR, "tags" VARCHAR, "timestamp"  BIGINT, "type" VARCHAR ,"typeConfig" VARCHAR, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS devices ("make" VARCHAR , "model" VARCHAR, "dataSourceId" BIGINT , CONSTRAINT pk PRIMARY KEY ("dataSourceId"))
CREATE TABLE IF NOT EXISTS parser_info ("id" BIGINT NOT NULL, "name" VARCHAR(256) ,"version" BIGINT, "className" VARCHAR , "jarStoragePath" VARCHAR ,"parserSchema" VARCHAR, "timestamp"  BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS files ("id" BIGINT NOT NULL, "name" VARCHAR(256) ,"version" BIGINT, "auxiliaryInfo" VARCHAR ,"storedFileName" VARCHAR , "timestamp"  BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS datafeeds ("id" BIGINT NOT NULL, "dataSourceId" BIGINT , "name" VARCHAR(256) , "description" VARCHAR, "tags" VARCHAR, "parserId" BIGINT , "type" VARCHAR , CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topologies ("id" BIGINT NOT NULL, "name" VARCHAR (256), "config" VARCHAR, "timestamp"  BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topology_components ("id" BIGINT NOT NULL, "name" VARCHAR(256), "type" VARCHAR, "subType" VARCHAR, "streamingEngine" VARCHAR, "config" VARCHAR, "schemaClass" VARCHAR, "transformationClass" VARCHAR, "timestamp"  BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS topology_editor_metadata ("topologyId" BIGINT NOT NULL, "data" VARCHAR, "timestamp"  BIGINT, CONSTRAINT pk PRIMARY KEY ("topologyId"))
CREATE TABLE IF NOT EXISTS tag ("id" BIGINT NOT NULL, "name" VARCHAR(256), "description" VARCHAR(256), "timestamp" BIGINT, CONSTRAINT pk PRIMARY KEY ("id"))
CREATE TABLE IF NOT EXISTS tag_storable_mapping ("tagId" BIGINT NOT NULL, "storableNamespace" VARCHAR(32) NOT NULL, "storableId" BIGINT NOT NULL, CONSTRAINT pk PRIMARY KEY ("tagId", "storableNamespace", "storableId"))
CREATE TABLE IF NOT EXISTS sequence_table ("id" VARCHAR, "datasources" BIGINT, "datafeeds" BIGINT, "parser_info" BIGINT, "files" BIGINT, "topologies" BIGINT, "topology_components" BIGINT, "tag" BIGINT,  CONSTRAINT pk PRIMARY KEY ("id"))
CREATE SEQUENCE IF NOT EXISTS datasources_sequence
CREATE SEQUENCE IF NOT EXISTS datafeeds_sequence
CREATE SEQUENCE IF NOT EXISTS parser_info_sequence
CREATE SEQUENCE IF NOT EXISTS topologies_sequence
CREATE SEQUENCE IF NOT EXISTS topology_components_sequence
CREATE SEQUENCE IF NOT EXISTS tag_sequence
CREATE SEQUENCE IF NOT EXISTS files_sequence
