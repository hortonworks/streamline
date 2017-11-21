-- Copyright 2017 Hortonworks.;
-- ;
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License;
-- You may obtain a copy of the License at;
-- ;
--    http://www.apache.org/licenses/LICENSE-2.0;
-- ;
-- Unless required by applicable law or agreed to in writing, software;
-- distributed under the License is distributed on an "AS IS" BASIS,;
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.;
-- See the License for the specific language governing permissions and;
-- limitations under the License.;

CREATE TABLE "component_process" (
  "id"            NUMBER(19,0)    NOT NULL,
  "componentId"   NUMBER(19,0)    NOT NULL,
  "host"          VARCHAR2(255)   NOT NULL,
  "protocol"      VARCHAR2(255),
  "port"          NUMBER(10,0),
  "timestamp"     NUMBER(19,0),
  CONSTRAINT component_process_pk PRIMARY KEY ("id"),
  CONSTRAINT component_process_fk_cmpnt FOREIGN KEY ("componentId") REFERENCES "component" ("id")
);

CREATE SEQUENCE "COMPONENT_PROCESS" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000;

CREATE OR REPLACE PROCEDURE put_rec_to_component_process AUTHID CURRENT_USER AS
component_process_id NUMBER;
BEGIN
    FOR ptr IN (SELECT "id", "hosts", "protocol", "port", "timestamp" FROM "component")
    LOOP
       FOR i IN (SELECT trim(regexp_substr(REGEXP_REPLACE(ptr."hosts",'\[|"|\]',''), '[^,]+', 1, LEVEL)) host FROM dual
         CONNECT BY regexp_substr(REGEXP_REPLACE(ptr."hosts",'\[\]\"','') , '[^,]+', 1, LEVEL) IS NOT NULL )
       LOOP
           component_process_id := COMPONENT_PROCESS.NEXTVAL;
           INSERT INTO "component_process" values (component_process_id, ptr."id", i.host, ptr."protocol", ptr."port", ptr."timestamp");
      END LOOP;
    END LOOP;
    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE;
END put_rec_to_component_process;

/

CALL put_rec_to_component_process();


ALTER TABLE "component" DROP ("hosts","protocol","port");