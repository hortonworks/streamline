-- THE NAMES OF THE TABLE COLUMNS MUST MATCH THE NAMES OF THE CORRESPONDING CLASS MODEL FIELDS;

CREATE TABLE IF NOT EXISTS dashboard (
  "id" SERIAL PRIMARY KEY,
  "name" VARCHAR(255) NOT NULL,
  "description" VARCHAR(256) NOT NULL,
  "data" TEXT NOT NULL,
  "timestamp"  BIGINT,
  CONSTRAINT dashboard_uk_name UNIQUE ("name")
);

CREATE TABLE IF NOT EXISTS ml_models (
  "id" SERIAL PRIMARY KEY,
  "name" VARCHAR(255) NOT NULL,
  "uploadedFileName" VARCHAR(256) NOT NULL,
  "pmml" TEXT NOT NULL,
  "timestamp"  BIGINT,
  CONSTRAINT ml_models_uk_name UNIQUE ("name")
);

CREATE TABLE IF NOT EXISTS widget (
  "id" SERIAL PRIMARY KEY,
  "name" VARCHAR(255) NOT NULL,
  "description" VARCHAR(256) NOT NULL,
  "type" VARCHAR(256) NOT NULL,
  "data" TEXT NOT NULL,
  "timestamp"  BIGINT,
  CONSTRAINT widget_uk_name UNIQUE ("name"),
  "dashboardId"  INTEGER REFERENCES dashboard
);

CREATE TABLE IF NOT EXISTS datasource (
  "id" SERIAL PRIMARY KEY,
  "name" VARCHAR(255) NOT NULL,
  "description" VARCHAR(256) NOT NULL,
  "type" VARCHAR(256) NOT NULL,
  "url" VARCHAR(256) NOT NULL,
  "data" TEXT NOT NULL,
  "timestamp"  BIGINT,
  "dashboardId"  INTEGER REFERENCES  dashboard,
  CONSTRAINT datasource_uk_name UNIQUE ("name")
);

CREATE TABLE IF NOT EXISTS widget_datasource_mapping (
  "widgetId" BIGINT REFERENCES  widget,
  "datasourceId" BIGINT REFERENCES datasource,
  PRIMARY KEY ("widgetId", "datasourceId")
);

CREATE TABLE IF NOT EXISTS parser_info (
    "id" SERIAL PRIMARY KEY,
    "name" VARCHAR(255) NOT NULL,
    "version" BIGINT,                             -- TODO: NOT NULL ???
    "className" TEXT NOT NULL,
    "jarStoragePath" TEXT NOT NULL,
    "parserSchema" TEXT NOT NULL,                 -- the schema is serialized to a String before storing in DB
    "timestamp"  BIGINT,
    UNIQUE ("name", "version")
);

CREATE TABLE IF NOT EXISTS files (
    "id" SERIAL PRIMARY KEY ,
    "name" VARCHAR(255) NOT NULL,
    "version" BIGINT NOT NULL,
    "storedFileName" TEXT NOT NULL,
    "description" TEXT,
    "timestamp"  BIGINT,
    UNIQUE ("name", "version")
);

CREATE TABLE IF NOT EXISTS namespaces (
     "id" SERIAL PRIMARY KEY ,
     "name" VARCHAR(256) NOT NULL,
     "streamingEngine" VARCHAR(256) NOT NULL,
     "timeSeriesDB" VARCHAR(256) NULL,
     "description" VARCHAR(256),
     "timestamp" BIGINT
);

CREATE TABLE IF NOT EXISTS namespace_service_cluster_mapping (
     "namespaceId" BIGINT NOT NULL,
     "serviceName" VARCHAR(255) NOT NULL,
     "clusterId" BIGINT NOT NULL,
     PRIMARY KEY ("namespaceId", "serviceName", "clusterId")
);

CREATE TABLE IF NOT EXISTS topology_versioninfos (
  "id" SERIAL PRIMARY KEY,
  "topologyId" BIGINT NOT NULL,
  "name" VARCHAR(256) NOT NULL,
  "description" TEXT NOT NULL,
  "timestamp"  BIGINT
);

CREATE TABLE IF NOT EXISTS topologies (
    "id" SERIAL NOT NULL,
    "versionId" BIGINT REFERENCES topology_versioninfos,
    "name" VARCHAR(256) NOT NULL,
    "description" TEXT,
    "namespaceId" BIGINT REFERENCES namespaces,
    "config" TEXT NOT NULL,
    PRIMARY KEY ("id", "versionId")
);

CREATE TABLE IF NOT EXISTS topology_component_bundles (
    "id" SERIAL PRIMARY KEY ,
    "name" VARCHAR(256) NOT NULL,
    "type" TEXT NOT NULL,
    "subType" TEXT NOT NULL,
    "streamingEngine" TEXT NOT NULL,
    "topologyComponentUISpecification" TEXT NOT NULL,
    "fieldHintProviderClass" TEXT,
    "transformationClass" TEXT,
    "timestamp"  BIGINT,
    "bundleJar" TEXT,
    "builtin" CHAR(4),
    "mavenDeps" TEXT
);

CREATE TABLE IF NOT EXISTS topology_editor_metadata (
    "topologyId" BIGINT NOT NULL,
    "versionId" BIGINT REFERENCES topology_versioninfos,
    "data" TEXT NOT NULL,
    "timestamp" BIGINT,
    PRIMARY KEY ("topologyId", "versionId")
);

CREATE TABLE IF NOT EXISTS tag (
    "id" SERIAL PRIMARY KEY,
    "name" VARCHAR(255) NOT NULL,
    "description" TEXT NOT NULL,
    "timestamp" BIGINT,
    CONSTRAINT tag_uk UNIQUE ("name")
);

CREATE TABLE IF NOT EXISTS tag_storable_mapping (
    "tagId" BIGINT NOT NULL,
    "storableNamespace" VARCHAR(32) NOT NULL,
    "storableId" BIGINT NOT NULL,
    PRIMARY KEY ("tagId", "storableNamespace", "storableId")
);

CREATE TABLE IF NOT EXISTS streaminfo (
    "id" SERIAL NOT NULL,
    "versionId" BIGINT REFERENCES topology_versioninfos,
    "topologyId" BIGINT NOT NULL,
    "streamId" VARCHAR(255) NOT NULL,
    "description" TEXT,
    "fieldsData" TEXT NOT NULL,
    PRIMARY KEY("id", "versionId"),
    CONSTRAINT UK_streamId UNIQUE ("topologyId", "versionId", "streamId")
);

CREATE TABLE IF NOT EXISTS notifierinfos (
     "id" SERIAL PRIMARY KEY ,
     "name" VARCHAR(256) NOT NULL,
     "description" TEXT NOT NULL,
     "jarFileName" TEXT NOT NULL,
     "className" TEXT NOT NULL,
     "timestamp"  BIGINT,
     "properties" TEXT,
     "fieldValues" TEXT
);

CREATE TABLE IF NOT EXISTS topology_components (
    "id" SERIAL NOT NULL,
    "versionId" BIGINT NOT NULL,
    "topologyId" BIGINT,
    "topologyComponentBundleId" BIGINT,
    "name" VARCHAR(256),
    "description" TEXT,
    "configData" TEXT,
    PRIMARY KEY ("id", "versionId")
);

CREATE TABLE IF NOT EXISTS topology_sources (
    "id" SERIAL NOT NULL,
    "versionId" BIGINT REFERENCES topology_versioninfos,
    "topologyId" BIGINT NOT NULL,
    "topologyComponentBundleId" BIGINT NOT NULL,
    "name" VARCHAR(256) NOT NULL,
    "description" TEXT,
    "configData" TEXT NOT NULL,
    PRIMARY KEY ("id", "versionId")
);

CREATE TABLE IF NOT EXISTS topology_sinks (
  "id" SERIAL NOT NULL,
  "versionId" BIGINT REFERENCES topology_versioninfos,
  "topologyId" BIGINT NOT NULL,
  "topologyComponentBundleId" BIGINT NOT NULL,
  "name" VARCHAR(256) NOT NULL,
  "description" TEXT,
  "configData" TEXT NOT NULL,
  PRIMARY KEY ("id", "versionId")
);

CREATE TABLE IF NOT EXISTS topology_source_stream_mapping (
    "sourceId" BIGINT NOT NULL,
    "versionId" BIGINT NOT NULL,
    "streamId" BIGINT NOT NULL,
    PRIMARY KEY ("sourceId", "versionId", "streamId"),
    FOREIGN KEY ("sourceId", "versionId") REFERENCES topology_sources("id", "versionId"),
    FOREIGN KEY ("streamId", "versionId") REFERENCES streaminfo("id", "versionId")
);



CREATE TABLE IF NOT EXISTS topology_processors (
    "id" SERIAL NOT NULL,
    "versionId" BIGINT NOT NULL,
    "topologyId" BIGINT NOT NULL,
    "topologyComponentBundleId" BIGINT NOT NULL,
    "name" VARCHAR(256) NOT NULL,
    "description" TEXT,
    "configData" TEXT NOT NULL,
    PRIMARY KEY ("id", "versionId"),
    FOREIGN KEY ("versionId") REFERENCES topology_versioninfos(id)
);

CREATE TABLE IF NOT EXISTS topology_processor_stream_mapping (
    "processorId" BIGINT NOT NULL,
    "versionId" BIGINT NOT NULL,
    "streamId" BIGINT NOT NULL,
    PRIMARY KEY ("processorId", "versionId", "streamId"),
    FOREIGN KEY ("processorId", "versionId") REFERENCES topology_processors("id", "versionId"),
    FOREIGN KEY ("streamId", "versionId") REFERENCES streaminfo("id", "versionId")
);

CREATE TABLE IF NOT EXISTS topology_edges (
    "id" SERIAL NOT NULL,
    "versionId" BIGINT NOT NULL,
    "topologyId" BIGINT NOT NULL,
    "fromId" BIGINT NOT NULL,
    "toId" BIGINT NOT NULL,
    "streamGroupingsData" TEXT NOT NULL,
    PRIMARY KEY ("id", "versionId"),
    FOREIGN KEY ("versionId") REFERENCES topology_versioninfos(id)
);

CREATE TABLE IF NOT EXISTS ruleinfos (
    "id" SERIAL NOT NULL,
    "versionId" BIGINT NOT NULL,
    "topologyId" BIGINT NOT NULL,
    "name" VARCHAR(256) NOT NULL,
    "description" TEXT NOT NULL,
    "streams" TEXT NULL,
    "outputStreams" TEXT NULL,
    "condition" TEXT NULL,
    "sql" TEXT NULL,
    "parsedRuleStr" TEXT NOT NULL,
    "projections" TEXT NOT NULL,
    "window" TEXT NOT NULL,
    "actions" TEXT NOT NULL,
    PRIMARY KEY ("id", "versionId"),
    FOREIGN KEY ("versionId") REFERENCES topology_versioninfos(id)
);

CREATE TABLE IF NOT EXISTS branchruleinfos (
    "id" SERIAL NOT NULL,
    "versionId" BIGINT NOT NULL,
    "topologyId" BIGINT NOT NULL,
    "name" VARCHAR(256) NOT NULL,
    "description" TEXT NOT NULL,
    "stream" TEXT NOT NULL,
    "outputStreams" TEXT NULL,
    "condition" TEXT NOT NULL,
    "parsedRuleStr" TEXT NOT NULL,
    "actions" TEXT NOT NULL,
    PRIMARY KEY ("id", "versionId"),
    FOREIGN KEY ("versionId") REFERENCES topology_versioninfos(id)
);

CREATE TABLE IF NOT EXISTS windowinfos (
    "id" SERIAL NOT NULL,
    "versionId" BIGINT NOT NULL,
    "topologyId" BIGINT NOT NULL,
    "name" VARCHAR(256) NOT NULL,
    "description" TEXT NOT NULL,
    "streams" TEXT NULL,
    "outputStreams" TEXT NULL,
    "condition" TEXT NULL,
    "parsedRuleStr" TEXT NOT NULL,
    "window" TEXT NOT NULL,
    "actions" TEXT NOT NULL,
    "projections" TEXT NULL,
    "groupbykeys" TEXT NULL,
    PRIMARY KEY ("id", "versionId"),
    FOREIGN KEY ("versionId") REFERENCES topology_versioninfos(id)
);

CREATE TABLE IF NOT EXISTS udfs (
    "id" SERIAL NOT NULL,
    "name" VARCHAR(256) NOT NULL,
    "displayName" VARCHAR(256) NOT NULL,
    "description" TEXT NOT NULL,
    "type"  VARCHAR(256) NOT NULL,
    "className"  VARCHAR(256) NOT NULL,
    "jarStoragePath"  VARCHAR(256) NOT NULL,
    "digest" VARCHAR(256) NOT NULL,
    "argTypes" VARCHAR(256) NOT NULL,
    "returnType" VARCHAR(256) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS clusters (
  "id" SERIAL NOT NULL,
  "name" VARCHAR(256) NOT NULL,
  "ambariImportUrl" TEXT,
  "description" TEXT,
  "timestamp" BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS services (
  "id" SERIAL NOT NULL,
  "clusterId" BIGINT NOT NULL,
  "name" VARCHAR(256) NOT NULL,
  "description" TEXT,
  "timestamp" BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS service_configurations (
  "id" SERIAL NOT NULL,
  "serviceId" BIGINT NOT NULL,
  "name" VARCHAR(256) NOT NULL,
  "configuration" TEXT NOT NULL,
  "description" TEXT,
  "filename" VARCHAR(256),
  "timestamp" BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS components (
  "id" SERIAL NOT NULL,
  "serviceId" BIGINT NOT NULL,
  "name" VARCHAR(256) NOT NULL,
  "hosts" TEXT NOT NULL,
  "protocol" VARCHAR(256),
  "port" INTEGER,
  "timestamp" BIGINT,
  PRIMARY KEY (id)
);
