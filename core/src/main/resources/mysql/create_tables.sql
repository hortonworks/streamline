--  CREATE DATABASE IF NOT EXISTS iotas;
--  USE iotas;

-- THE NAMES OF THE TABLE COLUMNS MUST MATCH THE NAMES OF THE CORRESPONDING CLASS MODEL FIELDS

 CREATE TABLE IF NOT EXISTS datasources (
     id BIGINT AUTO_INCREMENT NOT NULL,
     name VARCHAR(256) NOT NULL,
     description TEXT,
     tags TEXT,
     timestamp  BIGINT,
     type TEXT NOT NULL,
     typeConfig TEXT,
     PRIMARY KEY (id)
 );

CREATE TABLE IF NOT EXISTS devices (
    make VARCHAR(256) NOT NULL,
    model VARCHAR(256) NOT NULL,
    dataSourceId BIGINT NOT NULL,
    PRIMARY KEY (dataSourceId),
    UNIQUE KEY `UK_id_version` (make, model),
    FOREIGN KEY (dataSourceId) REFERENCES datasources(id)
);

CREATE TABLE IF NOT EXISTS parser_info (
    id BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(256) NOT NULL,
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
    name VARCHAR(256) NOT NULL,
    version BIGINT NOT NULL,
    storedFileName TEXT NOT NULL,
    auxiliaryInfo TEXT,
    timestamp  BIGINT,
    PRIMARY KEY (id),
    UNIQUE KEY `jars_UK_name_version` (name, version)
);

CREATE TABLE IF NOT EXISTS datafeeds (
    id BIGINT AUTO_INCREMENT NOT NULL,
    dataSourceId BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT,
    tags TEXT,
    parserId BIGINT NOT NULL,
    type TEXT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (dataSourceId) REFERENCES datasources(id),
    FOREIGN KEY (parserId) REFERENCES parser_info(id)
);

CREATE TABLE IF NOT EXISTS topologies (
    id BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(256) NOT NULL,
    config TEXT NOT NULL,
    timestamp  BIGINT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS topology_components (
    id BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(256) NOT NULL,
    type TEXT NOT NULL,
    subType TEXT NOT NULL,
    streamingEngine TEXT NOT NULL,
    config TEXT NOT NULL,
    schemaClass TEXT,
    transformationClass TEXT,
    timestamp  BIGINT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS topology_editor_metadata (
    topologyId BIGINT NOT NULL,
    data TEXT NOT NULL,
    timestamp BIGINT,
    PRIMARY KEY (topologyId)
);

CREATE TABLE IF NOT EXISTS tag (
    id BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description VARCHAR(256) NOT NULL,
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
    streamId VARCHAR(256) NOT NULL,
    fieldsData TEXT NOT NULL,
    timestamp BIGINT,
    UNIQUE KEY `UK_streamId` (streamId),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS notifierinfos (
     id BIGINT AUTO_INCREMENT NOT NULL,
     name VARCHAR(256) NOT NULL,
     jarFileName TEXT NOT NULL,
     className TEXT NOT NULL,
     timestamp  BIGINT,
     properties TEXT,
     fieldValues TEXT,
     PRIMARY KEY (id)
);
