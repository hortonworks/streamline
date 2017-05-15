-- THE NAMES OF THE TABLE COLUMNS MUST MATCH THE NAMES OF THE CORRESPONDING CLASS MODEL FIELDS;

CREATE TABLE IF NOT EXISTS dashboard (
  "id" SERIAL PRIMARY KEY,
  "name" VARCHAR(255) NOT NULL,
  "description" VARCHAR(256) NOT NULL,
  "data" TEXT NOT NULL,
  "timestamp"  BIGINT,
  CONSTRAINT dashboard_uk_name UNIQUE ("name")
);

CREATE TABLE IF NOT EXISTS ml_model (
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

CREATE TABLE IF NOT EXISTS file (
    "id" SERIAL PRIMARY KEY ,
    "name" VARCHAR(255) NOT NULL,
    "version" BIGINT NOT NULL,
    "storedFileName" TEXT NOT NULL,
    "description" TEXT,
    "timestamp"  BIGINT,
    UNIQUE ("name", "version")
);

CREATE TABLE IF NOT EXISTS namespace (
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

CREATE TABLE IF NOT EXISTS topology_version (
  "id" SERIAL PRIMARY KEY,
  "topologyId" BIGINT NOT NULL,
  "name" VARCHAR(256) NOT NULL,
  "description" TEXT NOT NULL,
  "timestamp"  BIGINT
);

CREATE TABLE IF NOT EXISTS topology (
    "id" SERIAL NOT NULL,
    "versionId" BIGINT REFERENCES topology_version,
    "name" VARCHAR(256) NOT NULL,
    "description" TEXT,
    "namespaceId" BIGINT REFERENCES namespace,
    "config" TEXT NOT NULL,
    PRIMARY KEY ("id", "versionId")
);

CREATE TABLE IF NOT EXISTS topology_component_bundle (
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
    "builtin" CHAR(5),
    "mavenDeps" TEXT
);

CREATE TABLE IF NOT EXISTS topology_editor_metadata (
    "topologyId" BIGINT NOT NULL,
    "versionId" BIGINT REFERENCES topology_version,
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

CREATE TABLE IF NOT EXISTS topology_stream (
    "id" SERIAL NOT NULL,
    "versionId" BIGINT REFERENCES topology_version,
    "topologyId" BIGINT NOT NULL,
    "streamId" VARCHAR(255) NOT NULL,
    "description" TEXT,
    "fieldsData" TEXT NOT NULL,
    PRIMARY KEY("id", "versionId"),
    CONSTRAINT UK_streamId UNIQUE ("topologyId", "versionId", "streamId")
);

CREATE TABLE IF NOT EXISTS notifier (
     "id" SERIAL PRIMARY KEY ,
     "name" VARCHAR(256) NOT NULL,
     "description" TEXT NOT NULL,
     "jarFileName" TEXT NOT NULL,
     "className" TEXT NOT NULL,
     "timestamp"  BIGINT,
     "properties" TEXT,
     "fieldValues" TEXT,
     "builtin" CHAR(5)
);

CREATE TABLE IF NOT EXISTS topology_component (
    "id" SERIAL NOT NULL,
    "versionId" BIGINT NOT NULL,
    "topologyId" BIGINT,
    "topologyComponentBundleId" BIGINT,
    "name" VARCHAR(256),
    "description" TEXT,
    "configData" TEXT,
    PRIMARY KEY ("id", "versionId")
);

CREATE TABLE IF NOT EXISTS topology_source (
    "id" SERIAL NOT NULL,
    "versionId" BIGINT REFERENCES topology_version,
    "topologyId" BIGINT NOT NULL,
    "topologyComponentBundleId" BIGINT NOT NULL,
    "name" VARCHAR(256) NOT NULL,
    "description" TEXT,
    "configData" TEXT NOT NULL,
    PRIMARY KEY ("id", "versionId")
);

CREATE TABLE IF NOT EXISTS topology_sink (
  "id" SERIAL NOT NULL,
  "versionId" BIGINT REFERENCES topology_version,
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
    FOREIGN KEY ("sourceId", "versionId") REFERENCES topology_source("id", "versionId"),
    FOREIGN KEY ("streamId", "versionId") REFERENCES topology_stream("id", "versionId")
);



CREATE TABLE IF NOT EXISTS topology_processor (
    "id" SERIAL NOT NULL,
    "versionId" BIGINT NOT NULL,
    "topologyId" BIGINT NOT NULL,
    "topologyComponentBundleId" BIGINT NOT NULL,
    "name" VARCHAR(256) NOT NULL,
    "description" TEXT,
    "configData" TEXT NOT NULL,
    PRIMARY KEY ("id", "versionId"),
    FOREIGN KEY ("versionId") REFERENCES topology_version(id)
);

CREATE TABLE IF NOT EXISTS topology_processor_stream_mapping (
    "processorId" BIGINT NOT NULL,
    "versionId" BIGINT NOT NULL,
    "streamId" BIGINT NOT NULL,
    PRIMARY KEY ("processorId", "versionId", "streamId"),
    FOREIGN KEY ("processorId", "versionId") REFERENCES topology_processor("id", "versionId"),
    FOREIGN KEY ("streamId", "versionId") REFERENCES topology_stream("id", "versionId")
);

CREATE TABLE IF NOT EXISTS topology_edge (
    "id" SERIAL NOT NULL,
    "versionId" BIGINT NOT NULL,
    "topologyId" BIGINT NOT NULL,
    "fromId" BIGINT NOT NULL,
    "toId" BIGINT NOT NULL,
    "streamGroupingsData" TEXT NOT NULL,
    PRIMARY KEY ("id", "versionId"),
    FOREIGN KEY ("versionId") REFERENCES topology_version(id)
);

CREATE TABLE IF NOT EXISTS topology_rule (
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
    FOREIGN KEY ("versionId") REFERENCES topology_version(id)
);

CREATE TABLE IF NOT EXISTS topology_branchrule (
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
    FOREIGN KEY ("versionId") REFERENCES topology_version(id)
);

CREATE TABLE IF NOT EXISTS topology_window (
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
    FOREIGN KEY ("versionId") REFERENCES topology_version(id)
);

CREATE TABLE IF NOT EXISTS udf (
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
    "builtin" CHAR(5),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS cluster (
  "id" SERIAL NOT NULL,
  "name" VARCHAR(256) NOT NULL,
  "ambariImportUrl" TEXT,
  "description" TEXT,
  "timestamp" BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS service (
  "id" SERIAL NOT NULL,
  "clusterId" BIGINT NOT NULL,
  "name" VARCHAR(256) NOT NULL,
  "description" TEXT,
  "timestamp" BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS service_configuration (
  "id" SERIAL NOT NULL,
  "serviceId" BIGINT NOT NULL,
  "name" VARCHAR(256) NOT NULL,
  "configuration" TEXT NOT NULL,
  "description" TEXT,
  "filename" VARCHAR(256),
  "timestamp" BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS component (
  "id" SERIAL NOT NULL,
  "serviceId" BIGINT NOT NULL,
  "name" VARCHAR(256) NOT NULL,
  "hosts" TEXT NOT NULL,
  "protocol" VARCHAR(256),
  "port" INTEGER,
  "timestamp" BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS topology_state (
  "topologyId" BIGINT NOT NULL,
  "name" VARCHAR(255) NOT NULL,
  "description" TEXT NOT NULL,
  PRIMARY KEY ("topologyId")
);

CREATE TABLE IF NOT EXISTS service_bundle (
  "id" SERIAL NOT NULL,
  "name" VARCHAR(256) NOT NULL,
  "serviceUISpecification" TEXT NOT NULL,
  "registerClass" TEXT,
  "timestamp" BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS acl_entry (
  "id"              SERIAL       NOT NULL,
  "objectId"        BIGINT       NOT NULL,
  "objectNamespace" VARCHAR(255) NOT NULL,
  "sidId"           BIGINT       NOT NULL,
  "sidType"         VARCHAR(255) NOT NULL,
  "permissions"     TEXT         NOT NULL,
  "owner"           BOOLEAN      NOT NULL,
  "grant"           BOOLEAN      NOT NULL,
  "timestamp"       BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS role (
  "id"        SERIAL       NOT NULL,
  "name"      VARCHAR(255) NOT NULL,
  "displayName" VARCHAR(256) NOT NULL,
  "description" TEXT,
  "system" BOOLEAN NOT NULL,
  "metadata" TEXT,
  "timestamp" BIGINT,
  CONSTRAINT UK_name_role UNIQUE ("name"),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS role_hierarchy (
  "parentId" BIGINT NOT NULL,
  "childId"  BIGINT NOT NULL,
  PRIMARY KEY ("parentId", "childId"),
  FOREIGN KEY ("parentId") REFERENCES role ("id"),
  FOREIGN KEY ("childId") REFERENCES role ("id")
);

CREATE TABLE IF NOT EXISTS user_entry (
  "id"        SERIAL       NOT NULL,
  "name"      VARCHAR(255) NOT NULL,
  "email"     VARCHAR(255) NOT NULL,
  "metadata" TEXT,
  "timestamp" BIGINT,
  CONSTRAINT UK_name_user UNIQUE ("name"),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS user_role (
  "userId" BIGINT NOT NULL,
  "roleId" BIGINT NOT NULL,
  PRIMARY KEY ("userId", "roleId"),
  FOREIGN KEY ("userId") REFERENCES "user_entry" (id),
  FOREIGN KEY ("roleId") REFERENCES "role" (id)
);

CREATE TABLE IF NOT EXISTS topology_editor_toolbar (
  "userId" BIGINT NOT NULL REFERENCES "user_entry" (id),
  "data" TEXT NOT NULL,
  "timestamp" BIGINT,
  PRIMARY KEY ("userId")
);

CREATE TABLE IF NOT EXISTS topology_test_run_case (
  "id" SERIAL NOT NULL,
  "name" VARCHAR(256) NOT NULL,
  "topologyId" BIGINT NOT NULL,
  "timestamp" BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS topology_test_run_case_source (
  "id" SERIAL NOT NULL,
  "testCaseId" BIGINT NOT NULL,
  "sourceId" BIGINT NOT NULL,
  "records" TEXT NOT NULL,
  "occurrence" BIGINT NOT NULL,
  "timestamp" BIGINT,
  PRIMARY KEY (id),
  FOREIGN KEY ("testCaseId") REFERENCES topology_test_run_case(id),
  CONSTRAINT UK_testcase_source UNIQUE ("testCaseId", "sourceId")
);

CREATE TABLE IF NOT EXISTS topology_test_run_case_sink (
  "id" SERIAL NOT NULL,
  "testCaseId" BIGINT NOT NULL,
  "sinkId" BIGINT NOT NULL,
  "records" TEXT NOT NULL,
  "timestamp" BIGINT,
  PRIMARY KEY (id),
  FOREIGN KEY ("testCaseId") REFERENCES topology_test_run_case(id),
  CONSTRAINT UK_testcase_sink UNIQUE ("testCaseId", "sinkId")
);

CREATE TABLE IF NOT EXISTS topology_test_run_histories (
  "id" SERIAL NOT NULL,
  "topologyId" BIGINT NOT NULL,
  "versionId" BIGINT,
  "testRecords" TEXT NOT NULL,
  "finished" CHAR(5) NOT NULL,
  "success" CHAR(5) NOT NULL,
  "expectedOutputRecords" TEXT,
  "actualOutputRecords" TEXT,
  "matched" CHAR(5),
  "eventLogFilePath" VARCHAR(256) NOT NULL,
  "startTime" BIGINT,
  "finishTime" BIGINT,
  "timestamp" BIGINT,
  PRIMARY KEY (id),
  FOREIGN KEY ("topologyId", "versionId") REFERENCES topology("id", "versionId")
);
