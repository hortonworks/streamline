-- Copyright 2017 Hortonworks.;
-- ;
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License;
-- You may obtain a copy of the License at;
-- ;
--    http://www.apache.org/licenses/LICENSE-2.0;
-- ;
-- Unless required by applicable law or agreed to in writing, software;
-- distributed under the License is distributed on an "AS IS" BASIS,;
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.;
-- See the License for the specific language governing permissions and;
-- limitations under the License.;

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
  "versionId" BIGINT NOT NULL,
  "timestamp" BIGINT,
  PRIMARY KEY ("id"),
  FOREIGN KEY ("topologyId", "versionId") REFERENCES topology("id", "versionId")
);

CREATE TABLE IF NOT EXISTS topology_test_run_case_source (
  "id" SERIAL NOT NULL,
  "testCaseId" BIGINT NOT NULL,
  "sourceId" BIGINT NOT NULL,
  "versionId" BIGINT NOT NULL,
  "records" TEXT NOT NULL,
  "occurrence" BIGINT NOT NULL,
  "timestamp" BIGINT,
  PRIMARY KEY ("id"),
  FOREIGN KEY ("testCaseId") REFERENCES topology_test_run_case("id"),
  FOREIGN KEY ("sourceId", "versionId") REFERENCES topology_source("id", "versionId"),
  CONSTRAINT UK_testcase_source UNIQUE ("testCaseId", "sourceId", "versionId")
);

CREATE TABLE IF NOT EXISTS topology_test_run_case_sink (
  "id" SERIAL NOT NULL,
  "testCaseId" BIGINT NOT NULL,
  "sinkId" BIGINT NOT NULL,
  "versionId" BIGINT NOT NULL,
  "records" TEXT NOT NULL,
  "timestamp" BIGINT,
  PRIMARY KEY ("id"),
  FOREIGN KEY ("testCaseId") REFERENCES topology_test_run_case(id),
  FOREIGN KEY ("sinkId", "versionId") REFERENCES topology_sink("id", "versionId"),
  CONSTRAINT UK_testcase_sink UNIQUE ("testCaseId", "sinkId", "versionId")
);

CREATE TABLE IF NOT EXISTS topology_test_run_histories (
  "id" SERIAL NOT NULL,
  "topologyId" BIGINT NOT NULL,
  "versionId" BIGINT NOT NULL,
  "testCaseId" BIGINT NOT NULL,
  "finished" CHAR(5) NOT NULL,
  "success" CHAR(5) NOT NULL,
  "expectedOutputRecords" TEXT,
  "actualOutputRecords" TEXT,
  "matched" CHAR(5),
  "eventLogFilePath" VARCHAR(256) NOT NULL,
  "startTime" BIGINT,
  "finishTime" BIGINT,
  "timestamp" BIGINT,
  PRIMARY KEY ("id"),
  FOREIGN KEY ("topologyId", "versionId") REFERENCES topology("id", "versionId"),
  FOREIGN KEY ("testCaseId") REFERENCES topology_test_run_case("id")
);

-- Drop primary key from all tables whose table name is greater than 30 characters

CREATE OR REPLACE FUNCTION drop_primary_keys_from_table (param_table_name VARCHAR(255))
  RETURNS void AS $$
  DECLARE
    table_exist INT;
  BEGIN
    SELECT COUNT(*) INTO table_exist FROM information_schema."tables" WHERE table_schema = current_schema() and table_name = param_table_name;
    IF table_exist = 1 THEN
        EXECUTE CONCAT('ALTER TABLE "' || param_table_name || '" DROP CONSTRAINT "' || param_table_name ||'_pkey"');
    END IF;
  END;
$$ LANGUAGE plpgsql;

SELECT drop_primary_keys_from_table ('widget_datasource_mapping');
SELECT drop_primary_keys_from_table ('namespace_service_cluster_mapping');
SELECT drop_primary_keys_from_table ('tag_storable_mapping');
SELECT drop_primary_keys_from_table ('topology_source_stream_mapping');
SELECT drop_primary_keys_from_table ('topology_processor_stream_mapping');

-- Rename table whose table name is greater than 30 characters

CREATE OR REPLACE FUNCTION rename_table_if_exists (current_table_name VARCHAR(255), new_table_name VARCHAR(255))
  RETURNS void AS $$
  DECLARE
    cur_table_count INT;
    new_table_count INT;
  BEGIN
    SELECT COUNT(*) INTO cur_table_count FROM information_schema."tables" WHERE table_schema = current_schema() and table_name = current_table_name;
    SELECT COUNT(*) INTO new_table_count FROM information_schema."tables" WHERE table_schema = current_schema() and table_name = new_table_name;
    IF cur_table_count = 1 AND new_table_count = 0 THEN
      EXECUTE CONCAT('ALTER TABLE "'||current_table_name||'" RENAME TO "'||new_table_name||'"');
    ELSIF cur_table_count = 1 AND new_table_count = 1 THEN
      EXECUTE 'DROP TABLE "' || current_table_name || '" CASCADE';
    END IF;
  END;
$$ LANGUAGE plpgsql;

SELECT rename_table_if_exists ('widget_datasource_mapping','widget_datasource_map');
SELECT rename_table_if_exists ('namespace_service_cluster_mapping', 'namespace_service_cluster_map');
SELECT rename_table_if_exists ('tag_storable_mapping','tag_storable_map');
SELECT rename_table_if_exists ('topology_source_stream_mapping','topology_source_stream_map');
SELECT rename_table_if_exists ('topology_processor_stream_mapping','topology_processor_stream_map');

-- Add all the dropped primary key from table which have been renamed in previous step

CREATE OR REPLACE FUNCTION add_primary_key_if_not_exist (param_table_name VARCHAR(255), primary_keys VARCHAR(255))
  RETURNS void AS $$
  DECLARE
    pk_count INT;
  BEGIN
    SELECT COUNT(*) INTO pk_count FROM information_schema."table_constraints" WHERE table_schema = current_schema() and table_name = param_table_name and constraint_type='PRIMARY KEY';
    IF pk_count = 0 THEN
      EXECUTE 'ALTER TABLE IF EXISTS "' || param_table_name || '" ADD PRIMARY KEY ( ' || primary_keys || ')';
    END IF;
  END;
$$ LANGUAGE plpgsql;

SELECT add_primary_key_if_not_exist ('widget_datasource_map','"widgetId", "datasourceId"');
SELECT add_primary_key_if_not_exist ('namespace_service_cluster_map','"namespaceId", "serviceName", "clusterId"');
SELECT add_primary_key_if_not_exist ('tag_storable_map','"tagId", "storableNamespace", "storableId"');
SELECT add_primary_key_if_not_exist ('topology_source_stream_map','"sourceId", "versionId", "streamId"');
SELECT add_primary_key_if_not_exist ('topology_processor_stream_map','"processorId", "versionId", "streamId"');

-- Rename columns of table whose length is greater that 30 characters

CREATE OR REPLACE FUNCTION rename_if_exist(param_table_name VARCHAR, current_col VARCHAR, new_column VARCHAR)
  RETURNS void AS $$
  DECLARE
    col_count INT;
  BEGIN
    SELECT COUNT(*) INTO col_count FROM information_schema."columns" WHERE table_schema = current_schema() and table_name = param_table_name and column_name = current_col;
    IF col_count = 1 THEN
      EXECUTE 'ALTER TABLE IF EXISTS "' || param_table_name || '" RENAME COLUMN "' || current_col ||'" TO "' || new_column || '"';
    END IF;
  END;
$$ LANGUAGE plpgsql;

SELECT rename_if_exist('topology_component_bundle','topologyComponentUISpecification','topologyComponentUISpec');