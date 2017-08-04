CREATE TABLE IF NOT EXISTS fileblob (
    name VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL,
    data LONGBLOB NOT NULL,
    timestamp BIGINT,
    PRIMARY KEY (name)
);