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
    builtin CHAR(4),
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
  description VARCHAR(255) NOT NULL,
  PRIMARY KEY (topologyId)
);
