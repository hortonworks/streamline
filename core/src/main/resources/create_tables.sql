--  CREATE DATABASE IF NOT EXISTS iotas;
--  USE iotas;

-- THE NAMES OF THE TABLE COLUMNS MUST MATCH THE NAMES OF THE CORRESPONDING CLASS MODEL FIELDS

 CREATE TABLE IF NOT EXISTS datasources (
     dataSourceId BIGINT AUTO_INCREMENT NOT NULL,
     dataSourceName VARCHAR(256) NOT NULL,
     description TEXT,
     tags TEXT,
     timestamp  BIGINT,
     type TEXT NOT NULL,
     typeConfig TEXT,
     PRIMARY KEY (dataSourceId)
 );

CREATE TABLE IF NOT EXISTS devices (
    deviceId VARCHAR(256) NOT NULL,
    version BIGINT NOT NULL,
    dataSourceId BIGINT NOT NULL,
    PRIMARY KEY (dataSourceId),
    UNIQUE (deviceId, version),
    FOREIGN KEY (dataSourceId) REFERENCES datasources(dataSourceId)
);

CREATE TABLE IF NOT EXISTS parser_info (
    parserId BIGINT AUTO_INCREMENT NOT NULL,
    parserName VARCHAR(256) NOT NULL,
    version BIGINT,                             -- TODO: NOT NULL ???
    className TEXT NOT NULL,
    jarStoragePath TEXT NOT NULL,
    parserSchema TEXT NOT NULL,                 -- the schema is serialized to a String before storing in DB
    timestamp  BIGINT,
    PRIMARY KEY (parserId),
    UNIQUE (parserName)
);

CREATE TABLE IF NOT EXISTS datafeeds (
    dataFeedId BIGINT AUTO_INCREMENT NOT NULL,
    dataSourceId BIGINT NOT NULL,
    dataFeedName VARCHAR(256) NOT NULL,
    description TEXT,
    tags TEXT,
    parserId BIGINT NOT NULL,
    endpoint TEXT NOT NULL,
    timestamp  BIGINT,
    PRIMARY KEY (dataFeedId),
    FOREIGN KEY (dataSourceId) REFERENCES datasources(dataSourceId),
    FOREIGN KEY (parserId) REFERENCES parser_info(parserId)
);
