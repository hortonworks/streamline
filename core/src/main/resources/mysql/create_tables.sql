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
    id VARCHAR(256) NOT NULL,
    version BIGINT NOT NULL,
    dataSourceId BIGINT NOT NULL,
    PRIMARY KEY (dataSourceId),
    UNIQUE (id, version),
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
    UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS datafeeds (
    id BIGINT AUTO_INCREMENT NOT NULL,
    dataSourceId BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    description TEXT,
    tags TEXT,
    parserId BIGINT NOT NULL,
    endpoint TEXT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (dataSourceId) REFERENCES datasources(id),
    FOREIGN KEY (parserId) REFERENCES parser_info(id)
);

CREATE TABLE IF NOT EXISTS datastreams (
    id BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(256) NOT NULL,
    json VARCHAR(4096) NOT NULL,
    timestamp  BIGINT,
    PRIMARY KEY (id)
);
