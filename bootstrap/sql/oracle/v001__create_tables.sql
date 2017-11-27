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

-- User should have a explicit create table privileges instead of having said privilege through roles.
-- For example :- The user if he has "resource" role below migration will still fail if the user doesn't have explicit "create table" privilege.

CREATE OR REPLACE PROCEDURE create_if_not_exists( object_type IN VARCHAR2, create_statement IN VARCHAR2 ) AUTHID CURRENT_USER IS
BEGIN
    DBMS_OUTPUT.put_line (create_statement);
    EXECUTE IMMEDIATE '' || create_statement;
EXCEPTION
    WHEN OTHERS THEN
        IF (object_type = 'TABLE' AND SQLCODE != -955) OR (object_type = 'SEQUENCE' AND SQLCODE != -955) THEN
            RAISE;
        END IF;
END create_if_not_exists;

/


CALL create_if_not_exists('TABLE', 'CREATE TABLE "dashboard" (
  "id"          NUMBER(19,0)     NOT NULL,
  "name"        VARCHAR2(255)    NOT NULL,
  "description" VARCHAR2(255)    NOT NULL,
  "data"        CLOB             NOT NULL,
  "timestamp"   NUMBER(19,0),
  CONSTRAINT dashboard_pk PRIMARY KEY ("id"),
  CONSTRAINT dashboard_uk_name UNIQUE ("name")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "ml_model" (
  "id"               NUMBER(19,0)   NOT NULL,
  "name"              VARCHAR2(255)  NOT NULL,
  "uploadedFileName"  VARCHAR2(255)  NOT NULL,
  "pmml"              CLOB           NOT NULL,
  "timestamp"         NUMBER(19,0),
  CONSTRAINT ml_model_pk PRIMARY KEY ("id"),
  CONSTRAINT ml_model_uk_name UNIQUE ("name")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "widget" (
  "id"           NUMBER(19,0)   NOT NULL,
  "name"         VARCHAR2(255)  NOT NULL,
  "description"  VARCHAR2(255)  NOT NULL,
  "type"         VARCHAR2(255)  NOT NULL,
  "data"         CLOB           NOT NULL,
  "timestamp"    NUMBER(19,0),
  "dashboardId"  NUMBER(19,0)   NOT NULL,
  CONSTRAINT widget_pk PRIMARY KEY ("id"),
  CONSTRAINT widget_uk_name UNIQUE ("name"),
  CONSTRAINT widget_fk_dashboard FOREIGN KEY ("dashboardId") REFERENCES "dashboard" ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "datasource" (
  "id"           NUMBER(19,0)    NOT NULL,
  "name"         VARCHAR2(255)   NOT NULL,
  "description"  VARCHAR2(255)   NOT NULL,
  "type"         VARCHAR2(255)   NOT NULL,
  "url"          VARCHAR2(255)   NOT NULL,
  "data"         CLOB            NOT NULL,
  "timestamp"    NUMBER(19,0),
  "dashboardId"  NUMBER(19,0)    NOT NULL,
  CONSTRAINT datasource_pk PRIMARY KEY ("id"),
  CONSTRAINT datasource_uk_name UNIQUE ("name"),
  CONSTRAINT datasource_fk_dashboard FOREIGN KEY ("dashboardId") REFERENCES "dashboard" ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "widget_datasource_map" (
  "widgetId"       NUMBER(19,0)   NOT NULL,
  "datasourceId"   NUMBER(19,0)   NOT NULL,
  CONSTRAINT widget_ds_map_pk PRIMARY KEY ("widgetId","datasourceId"),
  CONSTRAINT widget_ds_map_fk_widget FOREIGN KEY ("widgetId") REFERENCES "widget" ("id"),
  CONSTRAINT widget_ds_map_fk_datasource FOREIGN KEY ("datasourceId") REFERENCES "datasource" ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "file" (
  "id"              NUMBER(19,0)    NOT NULL,
  "name"            VARCHAR2(255)   NOT NULL,
  "version"         NUMBER(19,0)    NOT NULL,
  "storedFileName"  VARCHAR2(4000)  NOT NULL,
  "description"     VARCHAR2(4000),
  "timestamp"       NUMBER(19,0),
  CONSTRAINT file_pk PRIMARY KEY ("id"),
  CONSTRAINT file_uk_name_version UNIQUE("name","version")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "namespace" (
  "id"              NUMBER(19,0)    NOT NULL,
  "name"            VARCHAR2(255)   NOT NULL,
  "streamingEngine" VARCHAR2(255)   NOT NULL,
  "timeSeriesDB"    VARCHAR2(255)   NULL,
  "description"     VARCHAR2(255),
  "timestamp"       NUMBER(19,0),
  CONSTRAINT namespace_pk PRIMARY KEY ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "namespace_service_cluster_map" (
  "namespaceId"    NUMBER(19,0)    NOT NULL,
  "serviceName"    VARCHAR2(255)   NOT NULL,
  "clusterId"      NUMBER(19,0)    NOT NULL,
  CONSTRAINT namespace_scm_pk PRIMARY KEY ("namespaceId", "serviceName", "clusterId")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_version" (
  "id"           NUMBER(19,0)   NOT NULL,
  "topologyId"   NUMBER(19,0)   NOT NULL,
  "name"         VARCHAR2(255)  NOT NULL,
  "description"  VARCHAR2(4000) NOT NULL,
  "timestamp"    NUMBER(19,0),
  CONSTRAINT topology_version_pk PRIMARY KEY ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology" (
  "id"           NUMBER(19,0)   NOT NULL,
  "versionId"    NUMBER(19,0)   NOT NULL,
  "name"         VARCHAR2(255)  NOT NULL,
  "description"  VARCHAR2(4000),
  "namespaceId"  NUMBER(19,0)   NOT NULL,
  "config"       CLOB           NOT NULL,
  CONSTRAINT topology_pk PRIMARY KEY ("id","versionId"),
  CONSTRAINT topology_fk_topology_version FOREIGN KEY ("versionId") REFERENCES "topology_version" ("id"),
  CONSTRAINT topology_fk_namespace FOREIGN KEY ("namespaceId") REFERENCES "namespace"("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_component_bundle" (
  "id"                               NUMBER(19,0)    NOT NULL,
  "name"                             VARCHAR2(255)   NOT NULL,
  "type"                             VARCHAR2(4000)  NOT NULL,
  "subType"                          VARCHAR2(4000)  NOT NULL,
  "streamingEngine"                  VARCHAR2(4000)  NOT NULL,
  "topologyComponentUISpec"          CLOB            NOT NULL,
  "fieldHintProviderClass"           VARCHAR2(4000),
  "transformationClass"              VARCHAR2(4000),
  "timestamp"                        NUMBER(19,0),
  "bundleJar"                        VARCHAR2(4000),
  "builtin"                          CHAR(5),
  "mavenDeps"                        VARCHAR2(4000),
  CONSTRAINT topology_component_bundle_pk PRIMARY KEY ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_editor_metadata" (
    "topologyId"   NUMBER(19,0) NOT NULL,
    "versionId"    NUMBER(19,0) NOT NULL,
    "data"         CLOB         NOT NULL,
    "timestamp"    NUMBER(19,0),
    CONSTRAINT topology_em_pk PRIMARY KEY ("topologyId", "versionId"),
    CONSTRAINT topology_em_fk_topology_ver FOREIGN KEY ("versionId") REFERENCES "topology_version" ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "tag" (
    "id"           NUMBER(19,0)    NOT NULL,
    "name"         VARCHAR2(255)   NOT NULL,
    "description"  VARCHAR2(4000)  NOT NULL,
    "timestamp"    NUMBER(19,0),
    CONSTRAINT tag_pk PRIMARY KEY ("id"),
    CONSTRAINT tag_uk_name UNIQUE ("name")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "tag_storable_map" (
    "tagId"              NUMBER(19,0)  NOT NULL,
    "storableNamespace"  VARCHAR2(32)  NOT NULL,
    "storableId"         NUMBER(19,0)  NOT NULL,
    CONSTRAINT tag_storable_mapping_pk PRIMARY KEY ("tagId", "storableNamespace", "storableId")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_stream" (
    "id"           NUMBER(19,0)   NOT NULL,
    "versionId"    NUMBER(19,0)   NOT NULL,
    "topologyId"   NUMBER(19,0)   NOT NULL,
    "streamId"     VARCHAR2(255)  NOT NULL,
    "description"  VARCHAR2(4000),
    "fieldsData"   CLOB           NOT NULL,
    CONSTRAINT topology_str_pk PRIMARY KEY ("id","versionId"),
    CONSTRAINT topology_str_uk_stream_id UNIQUE ("topologyId", "versionId", "streamId"),
    CONSTRAINT topology_str_fk_tolpology_ver FOREIGN KEY ("versionId") REFERENCES "topology_version" ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "notifier" (
     "id"           NUMBER(19,0)    NOT NULL,
     "name"         VARCHAR2(255)   NOT NULL,
     "description"  VARCHAR2(4000)  NOT NULL,
     "jarFileName"  VARCHAR2(4000)  NOT NULL,
     "className"    VARCHAR2(4000)  NOT NULL,
     "timestamp"    NUMBER(19,0),
     "properties"   CLOB,
     "fieldValues"  CLOB,
     "builtin"      CHAR(5),
     CONSTRAINT notifier_pk PRIMARY KEY ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_component" (
    "id"                          NUMBER(19,0)   NOT NULL,
    "versionId"                   NUMBER(19,0)   NOT NULL,
    "topologyId"                  NUMBER(19,0),
    "topologyComponentBundleId"   NUMBER(19,0),
    "name"                        VARCHAR2(255),
    "description"                 VARCHAR2(4000),
    "configData"                  CLOB,
    CONSTRAINT topology_component_pk PRIMARY KEY ("id", "versionId")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_source" (
    "id"                         NUMBER(19,0) NOT NULL,
    "versionId"                  NUMBER(19,0) NOT NULL,
    "topologyId"                 NUMBER(19,0) NOT NULL,
    "topologyComponentBundleId"  NUMBER(19,0) NOT NULL,
    "name"                       VARCHAR2(255) NOT NULL,
    "description"                VARCHAR2(4000),
    "configData"                 CLOB NOT NULL,
    CONSTRAINT topology_src_pk PRIMARY KEY ("id", "versionId"),
    CONSTRAINT topology_src_fk_topology_ver FOREIGN KEY ("versionId") REFERENCES "topology_version" ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_source_stream_map" (
    "sourceId"     NUMBER(19,0)   NOT NULL,
    "versionId"    NUMBER(19,0)   NOT NULL,
    "streamId"     NUMBER(19,0)   NOT NULL,
    CONSTRAINT topology_ssm_pk PRIMARY KEY ("sourceId", "versionId", "streamId"),
    CONSTRAINT topology_ssm_fk_topology_src FOREIGN KEY ("sourceId", "versionId") REFERENCES "topology_source" ("id", "versionId"),
    CONSTRAINT topology_ssm_fk_topology_strm FOREIGN KEY ("streamId", "versionId") REFERENCES "topology_stream" ("id", "versionId")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_sink" (
    "id"                         NUMBER(19,0)  NOT NULL,
    "versionId"                  NUMBER(19,0)  NOT NULL,
    "topologyId"                 NUMBER(19,0)  NOT NULL,
    "topologyComponentBundleId"  NUMBER(19,0)  NOT NULL,
    "name"                       VARCHAR2(255) NOT NULL,
    "description"                VARCHAR2(4000),
    "configData"                 CLOB          NOT NULL,
    CONSTRAINT topology_sink_pk PRIMARY KEY ("id", "versionId"),
    CONSTRAINT topology_sink_fk_topology_ver FOREIGN KEY ("versionId") REFERENCES "topology_version" ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_processor" (
    "id"                          NUMBER(19,0)    NOT NULL,
    "versionId"                   NUMBER(19,0)    NOT NULL,
    "topologyId"                  NUMBER(19,0)    NOT NULL,
    "topologyComponentBundleId"   NUMBER(19,0)    NOT NULL,
    "name"                        VARCHAR2(255)   NOT NULL,
    "description"                 VARCHAR2(4000),
    "configData"                  CLOB            NOT NULL,
    CONSTRAINT topology_prsr_pk PRIMARY KEY ("id", "versionId"),
    CONSTRAINT topology_prsr_fk_topology_ver FOREIGN KEY ("versionId") REFERENCES "topology_version" ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_processor_stream_map" (
    "processorId"     NUMBER(19,0)    NOT NULL,
    "versionId"       NUMBER(19,0)    NOT NULL,
    "streamId"        NUMBER(19,0)    NOT NULL,
    CONSTRAINT topology_psm_pk PRIMARY KEY ("processorId", "versionId", "streamId"),
    CONSTRAINT topology_psm_fk_topology_prsr FOREIGN KEY ("processorId", "versionId") REFERENCES "topology_processor" ("id", "versionId"),
    CONSTRAINT topology_psm_fk_topology_strm FOREIGN KEY ("streamId", "versionId") REFERENCES "topology_stream" ("id", "versionId")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_edge" (
    "id"                      NUMBER(19,0)    NOT NULL,
    "versionId"               NUMBER(19,0)    NOT NULL,
    "topologyId"              NUMBER(19,0)    NOT NULL,
    "fromId"                  NUMBER(19,0)    NOT NULL,
    "toId"                    NUMBER(19,0)    NOT NULL,
    "streamGroupingsData"     CLOB            NOT NULL,
    CONSTRAINT topology_edge_pk PRIMARY KEY ("id", "versionId"),
    CONSTRAINT topology_edge_fk_topology_ver FOREIGN KEY ("versionId") REFERENCES "topology_version" ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_rule" (
    "id"              NUMBER(19,0)    NOT NULL,
    "versionId"       NUMBER(19,0)    NOT NULL,
    "topologyId"      NUMBER(19,0)    NOT NULL,
    "name"            VARCHAR2(255)   NOT NULL,
    "description"     VARCHAR2(4000)  NOT NULL,
    "streams"         CLOB            NULL,
    "outputStreams"   CLOB            NULL,
    "condition"       CLOB            NULL,
    "sql"             CLOB            NULL,
    "parsedRuleStr"   CLOB            NOT NULL,
    "projections"     CLOB            NOT NULL,
    "window"          CLOB            NOT NULL,
    "actions"         CLOB            NOT NULL,
    CONSTRAINT topology_rule_pk PRIMARY KEY ("id", "versionId"),
    CONSTRAINT topology_rule_fk_topology_ver FOREIGN KEY ("versionId") REFERENCES "topology_version" ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_branchrule" (
    "id"              NUMBER(19,0)    NOT NULL,
    "versionId"       NUMBER(19,0)    NOT NULL,
    "topologyId"      NUMBER(19,0)    NOT NULL,
    "name"            VARCHAR2(255)   NOT NULL,
    "description"     VARCHAR2(4000)  NOT NULL,
    "stream"          CLOB            NOT NULL,
    "outputStreams"   CLOB            NULL,
    "condition"       CLOB            NOT NULL,
    "parsedRuleStr"   CLOB            NOT NULL,
    "actions"         CLOB            NOT NULL,
    CONSTRAINT topology_branchrule_pk PRIMARY KEY ("id", "versionId"),
    CONSTRAINT topology_br_fk_topology_ver FOREIGN KEY ("versionId") REFERENCES "topology_version" ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_window" (
    "id"              NUMBER(19,0)    NOT NULL,
    "versionId"       NUMBER(19,0)    NOT NULL,
    "topologyId"      NUMBER(19,0)    NOT NULL,
    "name"            VARCHAR2(255)   NOT NULL,
    "description"     VARCHAR2(4000)  NOT NULL,
    "streams"         CLOB            NULL,
    "outputStreams"   CLOB            NULL,
    "condition"       CLOB            NULL,
    "parsedRuleStr"   CLOB            NOT NULL,
    "window"          CLOB            NOT NULL,
    "actions"         CLOB            NOT NULL,
    "projections"     CLOB            NULL,
    "groupbykeys"     CLOB            NULL,
    CONSTRAINT topology_window_pk PRIMARY KEY ("id", "versionId"),
    CONSTRAINT topology_wdw_fk_topology_ver FOREIGN KEY ("versionId") REFERENCES "topology_version" ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "udf" (
    "id"              NUMBER(19,0)    NOT NULL,
    "name"            VARCHAR2(255)   NOT NULL,
    "displayName"     VARCHAR2(255)   NOT NULL,
    "description"     VARCHAR2(4000)  NOT NULL,
    "type"            VARCHAR2(255)   NOT NULL,
    "className"       VARCHAR2(255)   NOT NULL,
    "jarStoragePath"  VARCHAR2(255)   NOT NULL,
    "digest"          VARCHAR2(255)   NOT NULL,
    "argTypes"        VARCHAR2(255)   NOT NULL,
    "returnType"      VARCHAR2(255)   NOT NULL,
    "builtin"         CHAR(5),
    CONSTRAINT udf_pk PRIMARY KEY ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "cluster" (
  "id"                NUMBER(19,0)    NOT NULL,
  "name"              VARCHAR2(255)   NOT NULL,
  "ambariImportUrl"   VARCHAR2(4000),
  "description"       CLOB,
  "timestamp"         NUMBER(19,0),
  CONSTRAINT cluster_pk PRIMARY KEY ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "service" (
  "id"            NUMBER(19,0)    NOT NULL,
  "clusterId"     NUMBER(19,0)    NOT NULL,
  "name"          VARCHAR2(255)   NOT NULL,
  "description"   VARCHAR2(4000),
  "timestamp"     NUMBER(19,0),
  CONSTRAINT service_pk PRIMARY KEY ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "service_configuration" (
  "id"                NUMBER(19,0)    NOT NULL,
  "serviceId"         NUMBER(19,0)    NOT NULL,
  "name"              VARCHAR2(255)   NOT NULL,
  "configuration"     CLOB            NOT NULL,
  "description"       VARCHAR2(4000),
  "filename"          VARCHAR2(255),
  "timestamp"         NUMBER(19,0),
  CONSTRAINT service_configuration_pk PRIMARY KEY ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "component" (
  "id"            NUMBER(19,0)    NOT NULL,
  "serviceId"     NUMBER(19,0)    NOT NULL,
  "name"          VARCHAR2(255)   NOT NULL,
  "hosts"         VARCHAR2(255)   NOT NULL,
  "protocol"      VARCHAR2(255),
  "port"          NUMBER(10,0),
  "timestamp"     NUMBER(19,0),
  CONSTRAINT component_pk PRIMARY KEY ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_state" (
  "topologyId"    NUMBER(19,0)    NOT NULL,
  "name"          VARCHAR2(255)   NOT NULL,
  "description"   VARCHAR2(4000)  NOT NULL,
  CONSTRAINT topology_state_pk PRIMARY KEY ("topologyId")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "service_bundle" (
  "id"                        NUMBER(19,0)    NOT NULL,
  "name"                      VARCHAR2(255)   NOT NULL,
  "serviceUISpecification"    CLOB            NOT NULL,
  "registerClass"             VARCHAR2(4000),
  "timestamp"                 NUMBER(19,0),
  CONSTRAINT service_bundle_pk PRIMARY KEY ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "acl_entry" (
  "id"              NUMBER(19,0)          NOT NULL,
  "objectId"        NUMBER(19,0)          NOT NULL,
  "objectNamespace" VARCHAR2(255)         NOT NULL,
  "sidId"           NUMBER(19,0)          NOT NULL,
  "sidType"         VARCHAR2(255)         NOT NULL,
  "permissions"     CLOB                  NOT NULL,
  "owner"           NUMBER(1)             NOT NULL,
  "grant"           NUMBER(1)             NOT NULL,
  "timestamp"       NUMBER(19,0),
  CONSTRAINT acl_entry_pk PRIMARY KEY ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "role" (
  "id"                NUMBER(19,0)        NOT NULL,
  "name"              VARCHAR2(255)       NOT NULL,
  "displayName"       VARCHAR2(255)       NOT NULL,
  "description"       CLOB                NOT NULL,
  "system"            NUMBER(1)           NOT NULL,
  "metadata"          CLOB,
  "timestamp"         NUMBER(19,0),
  CONSTRAINT role_pk PRIMARY KEY ("id"),
  CONSTRAINT role_uk_name UNIQUE ("name")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "role_hierarchy" (
  "parentId"      NUMBER(19,0)    NOT NULL,
  "childId"       NUMBER(19,0)    NOT NULL,
  CONSTRAINT role_hierarchy_pk PRIMARY KEY ("parentId", "childId"),
  CONSTRAINT role_hierarchy_fk_role_parent FOREIGN KEY ("parentId") REFERENCES "role" ("id"),
  CONSTRAINT role_hierarchy_fk_role_child FOREIGN KEY ("childId") REFERENCES "role" ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "user_entry" (
  "id"            NUMBER(19,0)        NOT NULL,
  "name"          VARCHAR2(255)       NOT NULL,
  "email"         VARCHAR2(255)       NOT NULL,
  "metadata"      CLOB,
  "timestamp"     NUMBER(19,0),
  CONSTRAINT user_entry_pk PRIMARY KEY ("id"),
  CONSTRAINT user_entry_uk_name UNIQUE ("name")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "user_role" (
  "userId"        NUMBER(19,0)    NOT NULL,
  "roleId"        NUMBER(19,0)    NOT NULL,
  CONSTRAINT user_role_pk PRIMARY KEY ("userId", "roleId"),
  CONSTRAINT user_role_fk_user_entry FOREIGN KEY ("userId") REFERENCES "user_entry" ("id"),
  CONSTRAINT user_role_role FOREIGN KEY ("roleId") REFERENCES "role" ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_editor_toolbar" (
  "userId"        NUMBER(19,0)    NOT NULL,
  "data"          CLOB            NOT NULL,
  "timestamp"     NUMBER(19,0),
  CONSTRAINT topology_editor_toolbar_pk PRIMARY KEY ("userId"),
  CONSTRAINT topology_edr_tb_fk_user_entry FOREIGN KEY ("userId") REFERENCES "user_entry" ("id")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_test_run_case" (
  "id"            NUMBER(19,0)    NOT NULL,
  "name"          VARCHAR2(255)   NOT NULL,
  "topologyId"    NUMBER(19,0)    NOT NULL,
  "versionId"     NUMBER(19,0)    NOT NULL,
  "timestamp"     NUMBER(19,0),
  CONSTRAINT topology_test_run_case_pk PRIMARY KEY ("id"),
  CONSTRAINT topology_test_run_case_fk_tp FOREIGN KEY ("topologyId", "versionId") REFERENCES "topology" ("id", "versionId")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_test_run_case_source" (
  "id"                    NUMBER(19,0)    NOT NULL,
  "testCaseId"            NUMBER(19,0)    NOT NULL,
  "sourceId"              NUMBER(19,0)    NOT NULL,
  "versionId"             NUMBER(19,0)    NOT NULL,
  "records"               CLOB            NOT NULL,
  "occurrence"            NUMBER(10,0)    NOT NULL,
  "timestamp"             NUMBER(19,0),
  CONSTRAINT topology_trcs_pk PRIMARY KEY ("id"),
  CONSTRAINT topology_trcs_fk_topology_trc FOREIGN KEY ("testCaseId") REFERENCES "topology_test_run_case" ("id"),
  CONSTRAINT topology_trcs_fk_topology_src FOREIGN KEY ("sourceId", "versionId") REFERENCES "topology_source" ("id", "versionId"),
  CONSTRAINT topology_trcs_uk UNIQUE ("testCaseId", "sourceId", "versionId")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_test_run_case_sink" (
  "id"            NUMBER(19,0)    NOT NULL,
  "testCaseId"    NUMBER(19,0)    NOT NULL,
  "sinkId"        NUMBER(19,0)    NOT NULL,
  "versionId"     NUMBER(19,0)    NOT NULL,
  "records"       CLOB            NOT NULL,
  "timestamp"     NUMBER(19,0),
  CONSTRAINT topology_trcsk_pk PRIMARY KEY ("id"),
  CONSTRAINT topology_trcsk_fk_topology_tr FOREIGN KEY ("testCaseId") REFERENCES "topology_test_run_case" ("id"),
  CONSTRAINT topology_trcsk_fk_topology_sk FOREIGN KEY ("sinkId", "versionId") REFERENCES "topology_sink" ("id", "versionId"),
  CONSTRAINT topology_trcsk_uk UNIQUE ("testCaseId", "sinkId", "versionId")
)');

CALL create_if_not_exists('TABLE', 'CREATE TABLE "topology_test_run_histories" (
  "id"                        NUMBER(19,0)    NOT NULL,
  "topologyId"                NUMBER(19,0)    NOT NULL,
  "versionId"                 NUMBER(19,0)    NOT NULL,
  "testCaseId"                NUMBER(19,0)    NOT NULL,
  "finished"                  CHAR(5)         NOT NULL,
  "success"                   CHAR(5)         NOT NULL,
  "expectedOutputRecords"     CLOB,
  "actualOutputRecords"       CLOB,
  "matched"                   CHAR(5),
  "eventLogFilePath"          VARCHAR2(255)   NOT NULL,
  "startTime"                 NUMBER(19,0),
  "finishTime"                NUMBER(19,0),
  "timestamp"                 NUMBER(19,0),
  CONSTRAINT topology_trh_pk PRIMARY KEY ("id"),
  CONSTRAINT topology_trh_fk_topology FOREIGN KEY ("topologyId", "versionId") REFERENCES "topology" ("id", "versionId"),
  CONSTRAINT topology_trh_fk_topology_test FOREIGN KEY ("testCaseId") REFERENCES "topology_test_run_case" ("id")
)');


-- User should have CREATE SEQUENCE privilege to create sequnce which is will be used to get unique id for primary key

CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "DASHBOARD" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "ML_MODEL" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "WIDGET" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "DATASOURCE" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "FILE" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "NAMESPACE" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "TOPOLOGY_VERSION" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "TOPOLOGY" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "TOPOLOGY_COMPONENT_BUNDLE" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "TAG" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "TOPOLOGY_STREAM" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "NOTIFIER" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "TOPOLOGY_COMPONENT" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "TOPOLOGY_EDGE" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "TOPOLOGY_RULE" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "TOPOLOGY_BRANCHRULE" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "TOPOLOGY_WINDOW" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "UDF" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "CLUSTER" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "SERVICE" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "SERVICE_CONFIGURATION" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "COMPONENT" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "TOPOLOGY_STATE" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "SERVICE_BUNDLE" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "ACL_ENTRY" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "ROLE" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "TOPOLOGY_EDITOR_TOOLBAR" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "USER_ENTRY" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "TOPOLOGY_TEST_RUN_CASE" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "TOPOLOGY_TEST_RUN_CASE_SOURCE" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "TOPOLOGY_TEST_RUN_CASE_SINK" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');
CALL create_if_not_exists('SEQUENCE', 'CREATE SEQUENCE "TOPOLOGY_TEST_RUN_HISTORIES" START WITH 1 INCREMENT BY 1 MAXVALUE 10000000000000000000');