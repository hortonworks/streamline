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
  "id" SERIAL NOT NULL,
  "componentId" BIGINT NOT NULL,
  "host" VARCHAR(256) NOT NULL,
  "protocol" VARCHAR(256),
  "port" INTEGER,
  "timestamp" BIGINT,
  PRIMARY KEY (id),
  FOREIGN KEY ("componentId") REFERENCES component (id)
);

CREATE OR REPLACE FUNCTION migrate_records_to_component_process ()
  RETURNS void AS $$
  DECLARE
    ptr component%rowtype;
    host VARCHAR(255);
    cursor_component_process CURSOR FOR
       SELECT id, REPLACE( REPLACE( REPLACE(hosts,'[','' ),']','' ),'"','' ) as hosts, protocol, port, "timestamp" FROM component;
  BEGIN
    FOR ptr IN cursor_component_process LOOP
        FOREACH host IN ARRAY string_to_array(ptr.hosts, ',')
        LOOP
            INSERT INTO "component_process" ("componentId", "host", "protocol", "port", "timestamp") VALUES (ptr.id, host, ptr.protocol, ptr.port, ptr."timestamp");
        END LOOP;
    END LOOP;
  END;
$$ LANGUAGE plpgsql;

SELECT migrate_records_to_component_process ();

ALTER TABLE "component" DROP COLUMN "hosts", DROP COLUMN "protocol", DROP COLUMN "port";