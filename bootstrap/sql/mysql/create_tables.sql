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

CREATE TABLE IF NOT EXISTS parser_info (
    id BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(255) NOT NULL,
    version BIGINT,                             -- TODO: NOT NULL ???
    className TEXT NOT NULL,
    jarStoragePath TEXT NOT NULL,
    parserSchema TEXT NOT NULL,                 -- the schema is serialized to a String before storing in DB
    timestamp  BIGINT,
    PRIMARY KEY (id),
    UNIQUE KEY `UK_name_version` (name, version)
);

CREATE TABLE IF NOT EXISTS files (
    id BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL,
    storedFileName TEXT NOT NULL,
    description TEXT,
    timestamp  BIGINT,
    PRIMARY KEY (id),
    UNIQUE KEY `jars_UK_name_version` (name, version)
);

CREATE TABLE IF NOT EXISTS namespaces (
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

CREATE TABLE IF NOT EXISTS topology_versioninfos (
  id BIGINT AUTO_INCREMENT NOT NULL,
  topologyId BIGINT NOT NULL,
  name VARCHAR(256) NOT NULL,
  description TEXT NOT NULL,
  timestamp  BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS topologies (
    id BIGINT AUTO_INCREMENT NOT NULL,
    versionId BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT,
    namespaceId BIGINT NOT NULL,
    config TEXT NOT NULL,
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_versioninfos(id),
    FOREIGN KEY (namespaceId) REFERENCES namespaces(id)
);

CREATE TABLE IF NOT EXISTS topology_component_bundles (
    id BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(256) NOT NULL,
    type TEXT NOT NULL,
    subType TEXT NOT NULL,
    streamingEngine TEXT NOT NULL,
    topologyComponentUISpecification TEXT NOT NULL,
    schemaClass TEXT,
    transformationClass TEXT,
    timestamp  BIGINT,
    bundleJar TEXT,
    builtin CHAR(4),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS topology_editor_metadata (
    topologyId BIGINT NOT NULL,
    versionId BIGINT NOT NULL,
    data TEXT NOT NULL,
    timestamp BIGINT,
    PRIMARY KEY (topologyId, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_versioninfos(id)
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

CREATE TABLE IF NOT EXISTS streaminfo (
    id BIGINT AUTO_INCREMENT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT NOT NULL,
    streamId VARCHAR(255) NOT NULL,
    description TEXT,
    fieldsData TEXT NOT NULL,
    UNIQUE KEY `UK_streamId` (topologyId, versionId, streamId),
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_versioninfos(id)
);

CREATE TABLE IF NOT EXISTS notifierinfos (
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

CREATE TABLE IF NOT EXISTS topology_components (
    id BIGINT AUTO_INCREMENT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT,
    topologyComponentBundleId BIGINT,
    name VARCHAR(256),
    description TEXT,
    configData TEXT,
    PRIMARY KEY (id, versionId)
);

CREATE TABLE IF NOT EXISTS topology_sources (
    id BIGINT AUTO_INCREMENT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT NOT NULL,
    topologyComponentBundleId BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT,
    configData TEXT NOT NULL,
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_versioninfos(id)
);

CREATE TABLE IF NOT EXISTS topology_source_stream_mapping (
    sourceId BIGINT NOT NULL,
    versionId BIGINT NOT NULL,
    streamId BIGINT NOT NULL,
    PRIMARY KEY (sourceId, versionId, streamId),
    FOREIGN KEY (versionId) REFERENCES topology_versioninfos(id)
);

CREATE TABLE IF NOT EXISTS topology_sinks (
    id BIGINT AUTO_INCREMENT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT NOT NULL,
    topologyComponentBundleId BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT,
    configData TEXT NOT NULL,
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_versioninfos(id)
);

CREATE TABLE IF NOT EXISTS topology_processors (
    id BIGINT AUTO_INCREMENT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT NOT NULL,
    topologyComponentBundleId BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT,
    configData TEXT NOT NULL,
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_versioninfos(id)
);

CREATE TABLE IF NOT EXISTS topology_processor_stream_mapping (
    processorId BIGINT NOT NULL,
    versionId BIGINT NOT NULL,
    streamId BIGINT NOT NULL,
    PRIMARY KEY (processorId, versionId, streamId),
    FOREIGN KEY (versionId) REFERENCES topology_versioninfos(id)
);

CREATE TABLE IF NOT EXISTS topology_edges (
    id BIGINT AUTO_INCREMENT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT NOT NULL,
    fromId BIGINT NOT NULL,
    toId BIGINT NOT NULL,
    streamGroupingsData TEXT NOT NULL,
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_versioninfos(id)
);

CREATE TABLE IF NOT EXISTS ruleinfos (
    id BIGINT AUTO_INCREMENT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT NOT NULL,
    streams TEXT NULL,
    `condition` TEXT NULL,
    `sql` TEXT NULL,
    parsedRuleStr TEXT NOT NULL,
    projections TEXT NOT NULL,
    window TEXT NOT NULL,
    actions TEXT NOT NULL,
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_versioninfos(id)
);

CREATE TABLE IF NOT EXISTS branchruleinfos (
    id BIGINT AUTO_INCREMENT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT NOT NULL,
    stream TEXT NOT NULL,
    `condition` TEXT NOT NULL,
    parsedRuleStr TEXT NOT NULL,
    actions TEXT NOT NULL,
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_versioninfos(id)
);

CREATE TABLE IF NOT EXISTS windowinfos (
    id BIGINT AUTO_INCREMENT NOT NULL,
    versionId BIGINT NOT NULL,
    topologyId BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT NOT NULL,
    streams TEXT NULL,
    `condition` TEXT NULL,
    parsedRuleStr TEXT NOT NULL,
    window TEXT NOT NULL,
    actions TEXT NOT NULL,
    projections TEXT NULL,
    groupbykeys TEXT NULL,
    PRIMARY KEY (id, versionId),
    FOREIGN KEY (versionId) REFERENCES topology_versioninfos(id)
);

CREATE TABLE IF NOT EXISTS udfs (
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

CREATE TABLE IF NOT EXISTS clusters (
  id BIGINT AUTO_INCREMENT NOT NULL,
  name VARCHAR(256) NOT NULL,
  description TEXT,
  timestamp BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS services (
  id BIGINT AUTO_INCREMENT NOT NULL,
  clusterId BIGINT NOT NULL,
  name VARCHAR(256) NOT NULL,
  description TEXT,
  timestamp BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS service_configurations (
  id BIGINT AUTO_INCREMENT NOT NULL,
  serviceId BIGINT NOT NULL,
  name VARCHAR(256) NOT NULL,
  configuration TEXT NOT NULL,
  description TEXT,
  filename VARCHAR(256),
  timestamp BIGINT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS components (
  id BIGINT AUTO_INCREMENT NOT NULL,
  serviceId BIGINT NOT NULL,
  name VARCHAR(256) NOT NULL,
  hosts TEXT NOT NULL,
  protocol VARCHAR(256),
  port INTEGER,
  timestamp BIGINT,
  PRIMARY KEY (id)
);
