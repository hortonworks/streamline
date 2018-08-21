
 CREATE TABLE IF NOT EXISTS device_info (
     id BIGINT AUTO_INCREMENT NOT NULL,
     xid VARCHAR(256) NOT NULL,
     name VARCHAR(256) NOT NULL,
     version VARCHAR(256) NOT NULL,
     timestamp  BIGINT,
     PRIMARY KEY (id)
 );
