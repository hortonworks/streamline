-- Copyright 2017 Hortonworks.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--    http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

--  CREATE DATABASE IF NOT EXISTS streamline;
--  USE streamline;

-- THE NAMES OF THE TABLE COLUMNS MUST MATCH THE NAMES OF THE CORRESPONDING CLASS MODEL FIELDS;

CREATE TABLE IF NOT EXISTS dashboard (
  id BIGINT AUTO_INCREMENT NOT NULL,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(256) NOT NULL,
  data TEXT NOT NULL,
  timestamp  BIGINT,
  UNIQUE KEY `UK_name` (name),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS ml_model (
  id BIGINT AUTO_INCREMENT NOT NULL,
  name VARCHAR(255) NOT NULL,
  uploadedFileName VARCHAR(256) NOT NULL,
  pmml TEXT NOT NULL,
  timestamp  BIGINT,
  UNIQUE KEY `UK_name` (name),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS widget (
  id BIGINT AUTO_INCREMENT NOT NULL,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(256) NOT NULL,
  type VARCHAR(256) NOT NULL,
  data TEXT NOT NULL,
  timestamp  BIGINT,
  dashboardId  BIGINT NOT NULL,
  UNIQUE KEY `UK_name` (name),
  FOREIGN KEY (dashboardId) REFERENCES dashboard(id),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS datasource (
  id BIGINT AUTO_INCREMENT NOT NULL,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(256) NOT NULL,
  type VARCHAR(256) NOT NULL,
  url VARCHAR(256) NOT NULL,
  data TEXT NOT NULL,
  timestamp  BIGINT,
  dashboardId  BIGINT NOT NULL,
  UNIQUE KEY `UK_name` (name),
  FOREIGN KEY (dashboardId) REFERENCES dashboard(id),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS widget_datasource_mapping (
  widgetId BIGINT NOT NULL,
  datasourceId BIGINT NOT NULL,
  FOREIGN KEY (widgetId) REFERENCES widget(id),
  FOREIGN KEY (datasourceId) REFERENCES datasource(id),
  PRIMARY KEY (widgetId, datasourceId)
);

CREATE TABLE IF NOT EXISTS file (
    id BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL,
    storedFileName TEXT NOT NULL,
    description TEXT,
    timestamp  BIGINT,
    PRIMARY KEY (id),
    UNIQUE KEY `jars_UK_name_version` (name, version)
);

CREATE TABLE IF NOT EXISTS namespace (
       id BIGINT AUTO_INCREMENT NOT NULL,
       name VARCHAR(256) NOT NULL,
       streamingEngine VARCHAR(256) NOT NULL,
       timeSeriesDB VARCHAR(256) NULL,
       description VARCHAR(256),
       timestamp BIGINT,
       PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS namespace_service_cluster_mapping (
       namespaceId BIGINT NOT NULL,
       serviceName VARCHAR(255) NOT NULL,
       clusterId BIGINT NOT NULL,
       PRIMARY KEY (namespaceId, serviceName, clusterId)
);

CREATE TABLE IF NOT EXISTS topology_version (
  id BIGINT AUTO_INCREMENT NOT NULL,
  topologyId BIGINT NOT NULL,
  name VARCHAR(256) NOT NULL,
  description TEXT NOT NULL,
  timestamp  BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS topology (
    id BIGINT AUTO_INCREMENT NOT NULL,
    versionId BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT,
    namespaceId BIGINT NOT NULL,
    config TEXT NOT NULL,
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_version(id),
    FOREIGN KEY (namespaceId) REFERENCES namespace(id)
);

CREATE TABLE IF NOT EXISTS topology_component_bundle (
    id BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(256) NOT NULL,
    type TEXT NOT NULL,
    subType TEXT NOT NULL,
    streamingEngine TEXT NOT NULL,
    topologyComponentUISpecification TEXT NOT NULL,
    fieldHintProviderClass TEXT,
    transformationClass TEXT,
    timestamp  BIGINT,
    bundleJar TEXT,
    builtin CHAR(5),
    mavenDeps TEXT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS topology_editor_metadata (
    topologyId BIGINT NOT NULL,
    versionId BIGINT NOT NULL,
    data TEXT NOT NULL,
    timestamp BIGINT,
    PRIMARY KEY (topologyId, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_version(id)
);

CREATE TABLE IF NOT EXISTS tag (
    id BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    timestamp BIGINT,
    UNIQUE KEY `UK_name` (name),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS tag_storable_mapping (
    tagId BIGINT NOT NULL,
    storableNamespace VARCHAR(32) NOT NULL,
    storableId BIGINT NOT NULL,
    PRIMARY KEY (tagId, storableNamespace, storableId)
);

CREATE TABLE IF NOT EXISTS topology_stream (
    id BIGINT AUTO_INCREMENT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT NOT NULL,
    streamId VARCHAR(255) NOT NULL,
    description TEXT,
    fieldsData TEXT NOT NULL,
    UNIQUE KEY `UK_streamId` (topologyId, versionId, streamId),
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_version(id)
);

CREATE TABLE IF NOT EXISTS notifier (
     id BIGINT AUTO_INCREMENT NOT NULL,
     name VARCHAR(256) NOT NULL,
     description TEXT NOT NULL,
     jarFileName TEXT NOT NULL,
     className TEXT NOT NULL,
     timestamp  BIGINT,
     properties TEXT,
     fieldValues TEXT,
     builtin CHAR(5),
     PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS topology_component (
    id BIGINT AUTO_INCREMENT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT,
    topologyComponentBundleId BIGINT,
    name VARCHAR(256),
    description TEXT,
    configData TEXT,
    PRIMARY KEY (id, versionId)
);

CREATE TABLE IF NOT EXISTS topology_source (
    id BIGINT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT NOT NULL,
    topologyComponentBundleId BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT,
    configData TEXT NOT NULL,
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_version(id)
);

CREATE TABLE IF NOT EXISTS topology_source_stream_mapping (
    sourceId BIGINT NOT NULL,
    versionId BIGINT NOT NULL,
    streamId BIGINT NOT NULL,
    PRIMARY KEY (sourceId, versionId, streamId),
    FOREIGN KEY (sourceId, versionId) REFERENCES topology_source(id, versionId),
    FOREIGN KEY (streamId, versionId) REFERENCES topology_stream(id, versionId)
);

CREATE TABLE IF NOT EXISTS topology_sink (
    id BIGINT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT NOT NULL,
    topologyComponentBundleId BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT,
    configData TEXT NOT NULL,
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_version(id)
);

CREATE TABLE IF NOT EXISTS topology_processor (
    id BIGINT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT NOT NULL,
    topologyComponentBundleId BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT,
    configData TEXT NOT NULL,
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_version(id)
);

CREATE TABLE IF NOT EXISTS topology_processor_stream_mapping (
    processorId BIGINT NOT NULL,
    versionId BIGINT NOT NULL,
    streamId BIGINT NOT NULL,
    PRIMARY KEY (processorId, versionId, streamId),
    FOREIGN KEY (processorId, versionId) REFERENCES topology_processor(id, versionId),
    FOREIGN KEY (streamId, versionId) REFERENCES topology_stream(id, versionId)
);

CREATE TABLE IF NOT EXISTS topology_edge (
    id BIGINT AUTO_INCREMENT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT NOT NULL,
    fromId BIGINT NOT NULL,
    toId BIGINT NOT NULL,
    streamGroupingsData TEXT NOT NULL,
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_version(id)
);

CREATE TABLE IF NOT EXISTS topology_rule (
    id BIGINT AUTO_INCREMENT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT NOT NULL,
    streams TEXT NULL,
    outputStreams TEXT NULL,
    `condition` TEXT NULL,
    `sql` TEXT NULL,
    parsedRuleStr TEXT NOT NULL,
    projections TEXT NOT NULL,
    window TEXT NOT NULL,
    actions TEXT NOT NULL,
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_version(id)
);

CREATE TABLE IF NOT EXISTS topology_branchrule (
    id BIGINT AUTO_INCREMENT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT NOT NULL,
    stream TEXT NOT NULL,
    outputStreams TEXT NULL,
    `condition` TEXT NOT NULL,
    parsedRuleStr TEXT NOT NULL,
    actions TEXT NOT NULL,
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_version(id)
);

CREATE TABLE IF NOT EXISTS topology_window (
    id BIGINT AUTO_INCREMENT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT NOT NULL,
    streams TEXT NULL,
    outputStreams TEXT NULL,
    `condition` TEXT NULL,
    parsedRuleStr TEXT NOT NULL,
    window TEXT NOT NULL,
    actions TEXT NOT NULL,
    projections TEXT NULL,
    groupbykeys TEXT NULL,
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_version(id)
);

CREATE TABLE IF NOT EXISTS udf (
    id BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(256) NOT NULL,
    displayName VARCHAR(256) NOT NULL,
    description TEXT NOT NULL,
    type  VARCHAR(256) NOT NULL,
    className  VARCHAR(256) NOT NULL,
    jarStoragePath  VARCHAR(256) NOT NULL,
    digest VARCHAR(256) NOT NULL,
    argTypes VARCHAR(256) NOT NULL,
    returnType VARCHAR(256) NOT NULL,
    builtin CHAR(5),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS cluster (
  id BIGINT AUTO_INCREMENT NOT NULL,
  name VARCHAR(256) NOT NULL,
  ambariImportUrl TEXT,
  description TEXT,
  timestamp BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS service (
  id BIGINT AUTO_INCREMENT NOT NULL,
  clusterId BIGINT NOT NULL,
  name VARCHAR(256) NOT NULL,
  description TEXT,
  timestamp BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS service_configuration (
  id BIGINT AUTO_INCREMENT NOT NULL,
  serviceId BIGINT NOT NULL,
  name VARCHAR(256) NOT NULL,
  configuration TEXT NOT NULL,
  description TEXT,
  filename VARCHAR(256),
  timestamp BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS component (
  id BIGINT AUTO_INCREMENT NOT NULL,
  serviceId BIGINT NOT NULL,
  name VARCHAR(256) NOT NULL,
  hosts TEXT NOT NULL,
  protocol VARCHAR(256),
  port INTEGER,
  timestamp BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS topology_state (
  topologyId BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  description TEXT NOT NULL,
  PRIMARY KEY (topologyId)
);

CREATE TABLE IF NOT EXISTS service_bundle (
  id BIGINT AUTO_INCREMENT NOT NULL,
  name VARCHAR(256) NOT NULL,
  serviceUISpecification TEXT NOT NULL,
  registerClass TEXT,
  timestamp BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS acl_entry (
  id              BIGINT AUTO_INCREMENT NOT NULL,
  objectId        BIGINT                NOT NULL,
  objectNamespace VARCHAR(255)          NOT NULL,
  sidId           BIGINT                NOT NULL,
  sidType         VARCHAR(255)          NOT NULL,
  permissions     TEXT                  NOT NULL,
  owner           BOOLEAN               NOT NULL,
  `grant`         BOOLEAN               NOT NULL,
  timestamp       BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS role (
  id        BIGINT AUTO_INCREMENT NOT NULL,
  name      VARCHAR(255)          NOT NULL,
  displayName VARCHAR(255)          NOT NULL,
  description TEXT NOT NULL,
  system BOOLEAN NOT NULL,
  metadata TEXT,
  timestamp BIGINT,
  UNIQUE KEY `UK_name` (name),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS role_hierarchy (
  parentId BIGINT NOT NULL,
  childId  BIGINT NOT NULL,
  PRIMARY KEY (parentId, childId),
  FOREIGN KEY (parentId) REFERENCES role (id),
  FOREIGN KEY (childId) REFERENCES role (id)
);

CREATE TABLE IF NOT EXISTS user_entry (
  id    BIGINT AUTO_INCREMENT NOT NULL,
  name  VARCHAR(255)          NOT NULL,
  email VARCHAR(255)          NOT NULL,
  metadata TEXT,
  timestamp BIGINT,
  UNIQUE KEY `UK_name` (name),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS user_role (
  userId BIGINT NOT NULL,
  roleId BIGINT NOT NULL,
  PRIMARY KEY (userId, roleId),
  FOREIGN KEY (userId) REFERENCES user_entry (id),
  FOREIGN KEY (roleId) REFERENCES role (id)
);

CREATE TABLE IF NOT EXISTS topology_editor_toolbar (
  userId BIGINT NOT NULL,
  data TEXT NOT NULL,
  timestamp BIGINT,
  PRIMARY KEY (userId),
  FOREIGN KEY (userId) REFERENCES user_entry(id)
);

CREATE TABLE IF NOT EXISTS topology_test_run_case (
  id BIGINT AUTO_INCREMENT NOT NULL,
  name VARCHAR(256) NOT NULL,
  topologyId BIGINT NOT NULL,
  versionId BIGINT NOT NULL,
  timestamp BIGINT,
  PRIMARY KEY (id),
  FOREIGN KEY (topologyId, versionId) REFERENCES topology(id, versionId)
);

CREATE TABLE IF NOT EXISTS topology_test_run_case_source (
  id BIGINT AUTO_INCREMENT NOT NULL,
  testCaseId BIGINT NOT NULL,
  sourceId BIGINT NOT NULL,
  versionId BIGINT NOT NULL,
  records TEXT NOT NULL,
  occurrence INTEGER NOT NULL,
  timestamp BIGINT,
  PRIMARY KEY (id),
  FOREIGN KEY (testCaseId) REFERENCES topology_test_run_case(id),
  FOREIGN KEY (sourceId, versionId) REFERENCES topology_source(id, versionId),
  UNIQUE KEY `testcase_source` (testCaseId, sourceId, versionId)
);

CREATE TABLE IF NOT EXISTS topology_test_run_case_sink (
  id BIGINT AUTO_INCREMENT NOT NULL,
  testCaseId BIGINT NOT NULL,
  sinkId BIGINT NOT NULL,
  versionId BIGINT NOT NULL,
  records TEXT NOT NULL,
  timestamp BIGINT,
  PRIMARY KEY (id),
  FOREIGN KEY (testCaseId) REFERENCES topology_test_run_case(id),
  FOREIGN KEY (sinkId, versionId) REFERENCES topology_sink(id, versionId),
  UNIQUE KEY `testcase_sink` (testCaseId, sinkId, versionId)
);

CREATE TABLE IF NOT EXISTS topology_test_run_histories (
  id BIGINT AUTO_INCREMENT NOT NULL,
  topologyId BIGINT NOT NULL,
  versionId BIGINT NOT NULL,
  testCaseId BIGINT NOT NULL,
  finished CHAR(5) NOT NULL,
  success CHAR(5) NOT NULL,
  expectedOutputRecords TEXT,
  actualOutputRecords TEXT,
  matched CHAR(5),
  eventLogFilePath VARCHAR(256) NOT NULL,
  startTime BIGINT,
  finishTime BIGINT,
  timestamp BIGINT,
  PRIMARY KEY (id),
  FOREIGN KEY (topologyId, versionId) REFERENCES topology(id, versionId),
  FOREIGN KEY (testCaseId) REFERENCES topology_test_run_case(id)
);

-- Rename all table whose table name is greater than 30 characters

DROP PROCEDURE IF EXISTS rename_table_if_exists;

DELIMITER ///

CREATE PROCEDURE rename_table_if_exists (IN current_table_name VARCHAR(255), IN new_table_name VARCHAR(255))
BEGIN
    SELECT COUNT(*) INTO @current_table_count FROM information_schema.tables WHERE table_schema IN (SELECT DATABASE() FROM DUAL) AND table_type = 'BASE TABLE'  AND table_name = current_table_name;
    SELECT COUNT(*) INTO @new_table_count FROM information_schema.tables WHERE table_schema IN (SELECT DATABASE() FROM DUAL) AND table_type = 'BASE TABLE'  AND table_name = new_table_name;
    IF @current_table_count = 1 THEN
       IF @new_table_count = 0 THEN
          SET @str = CONCAT('RENAME TABLE `',current_table_name,'` TO `',new_table_name,'`');
          PREPARE stmt FROM @str;
          EXECUTE stmt;
          DEALLOCATE PREPARE stmt;
       ELSEIF @new_table_count = 1 THEN
          SET @str = CONCAT('DROP TABLE `',current_table_name,'`');
          PREPARE stmt FROM @str;
          EXECUTE stmt;
          DEALLOCATE PREPARE stmt;
       END IF;
    END IF;
END ///

DELIMITER ;


CALL rename_table_if_exists('widget_datasource_mapping', 'widget_datasource_map');
CALL rename_table_if_exists('namespace_service_cluster_mapping','namespace_service_cluster_map');
CALL rename_table_if_exists('tag_storable_mapping','tag_storable_map');
CALL rename_table_if_exists('topology_source_stream_mapping','topology_source_stream_map');
CALL rename_table_if_exists('topology_processor_stream_mapping','topology_processor_stream_map');

-- Rename all columns whose column name is greater than 30 characters


DROP PROCEDURE IF EXISTS alter_column_if_exists;

DELIMITER ///

CREATE PROCEDURE alter_column_if_exists (IN param_table_name VARCHAR(255), IN current_col_name VARCHAR(225), IN new_col_name VARCHAR(255), IN col_details VARCHAR(225))
BEGIN
    SELECT COUNT(*) INTO @col_exists FROM information_schema.columns WHERE table_schema IN (SELECT DATABASE() FROM DUAL) AND table_name = param_table_name AND column_name = current_col_name;
    IF @col_exists = 1 THEN
        SET @str = CONCAT('ALTER TABLE `',param_table_name,'` CHANGE COLUMN `',current_col_name,'` `', new_col_name, '`', col_details);
        PREPARE stmt FROM @str;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END ///

DELIMITER ;

CALL alter_column_if_exists ('topology_component_bundle', 'topologyComponentUISpecification', 'topologyComponentUISpec', 'TEXT NOT NULL');