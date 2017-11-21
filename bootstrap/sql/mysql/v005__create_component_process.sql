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

CREATE TABLE IF NOT EXISTS component_process (
  id BIGINT AUTO_INCREMENT NOT NULL,
  componentId BIGINT NOT NULL,
  host VARCHAR(255) NOT NULL,
  protocol VARCHAR(255),
  port INTEGER,
  `timestamp` BIGINT,
  PRIMARY KEY (id),
  FOREIGN KEY (componentId) REFERENCES component (id)
);


-- Move data from component to component_process

DROP PROCEDURE IF EXISTS migrate_records_to_component_process;

DELIMITER ///

CREATE PROCEDURE migrate_records_to_component_process ()
BEGIN
    DECLARE cursor_id BIGINT DEFAULT NULL;
    DECLARE cursor_hosts TEXT;
    DECLARE cursor_protocol VARCHAR(255) DEFAULT NULL;
    DECLARE cursor_port INTEGER DEFAULT NULL;
    DECLARE cursor_timestamp BIGINT DEFAULT NULL;
    DECLARE done INT DEFAULT FALSE;

    DECLARE inipos INTEGER;
    DECLARE endpos INTEGER;
    DECLARE maxlen INTEGER;
    DECLARE item VARCHAR(255);
    DECLARE mod_hosts VARCHAR(255);
    DECLARE delim VARCHAR(1);

    DECLARE cursor_component_process CURSOR FOR SELECT id, REPLACE( REPLACE( REPLACE(hosts,'[','' ),']','' ),'"','' ), protocol, port, timestamp FROM component;

    DECLARE continue handler FOR NOT FOUND SET done = true;

    DECLARE exit handler for sqlexception
        BEGIN
            ROLLBACK;
            RESIGNAL;
        END;

    START TRANSACTION;

    OPEN cursor_component_process;

    read_loop: LOOP
        FETCH cursor_component_process into cursor_id, cursor_hosts, cursor_protocol, cursor_port, cursor_timestamp;
        IF done THEN
            leave read_loop;
        END IF;

        SET delim = ',';
        SET inipos = 1;
        SET mod_hosts = CONCAT(cursor_hosts, delim);
        SET maxlen = LENGTH(mod_hosts);

        REPEAT
            SET endpos = LOCATE(delim, mod_hosts, inipos);
            SET item =  SUBSTR(mod_hosts, inipos, endpos - inipos);

            IF item <> '' AND item IS NOT NULL THEN
             INSERT INTO component_process (componentId, host, protocol, port, timestamp) VALUES (cursor_id, item, cursor_protocol, cursor_port, cursor_timestamp);
            END IF;
            SET inipos = endpos + 1;
        UNTIL inipos >= maxlen END REPEAT;
    END LOOP;

    CLOSE cursor_component_process;

    COMMIT;

END ///

DELIMITER ;


CALL migrate_records_to_component_process();

ALTER TABLE `component` DROP COLUMN `hosts`, DROP COLUMN `protocol`, DROP COLUMN `port`;

