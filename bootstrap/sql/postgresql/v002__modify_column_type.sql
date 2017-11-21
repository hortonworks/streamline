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


-- Change VARCHAR 256 TO 255

ALTER TABLE "dashboard" ALTER COLUMN "description" TYPE VARCHAR(255);
ALTER TABLE "ml_model" ALTER COLUMN "uploadedFileName" TYPE VARCHAR(255);
ALTER TABLE "widget" ALTER COLUMN "description" TYPE VARCHAR(255);
ALTER TABLE "widget" ALTER COLUMN "type" TYPE VARCHAR(255);
ALTER TABLE "datasource" ALTER COLUMN "description" TYPE VARCHAR(255);
ALTER TABLE "datasource" ALTER COLUMN "type" TYPE VARCHAR(255);
ALTER TABLE "datasource" ALTER COLUMN "url" TYPE VARCHAR(255);
ALTER TABLE "namespace" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "namespace" ALTER COLUMN "streamingEngine" TYPE VARCHAR(255);
ALTER TABLE "namespace" ALTER COLUMN "timeSeriesDB" TYPE VARCHAR(255);
ALTER TABLE "namespace" ALTER COLUMN "description" TYPE VARCHAR(255);
ALTER TABLE "topology_version" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "topology" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "topology_component_bundle" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "notifier" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "topology_component" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "topology_source" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "topology_sink" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "topology_processor" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "topology_rule" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "topology_branchrule" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "topology_window" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "udf" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "udf" ALTER COLUMN "displayName" TYPE VARCHAR(255);
ALTER TABLE "udf" ALTER COLUMN "type" TYPE VARCHAR(255);
ALTER TABLE "udf" ALTER COLUMN "className" TYPE VARCHAR(255);
ALTER TABLE "udf" ALTER COLUMN "jarStoragePath" TYPE VARCHAR(255);
ALTER TABLE "udf" ALTER COLUMN "digest" TYPE VARCHAR(255);
ALTER TABLE "udf" ALTER COLUMN "argTypes" TYPE VARCHAR(255);
ALTER TABLE "udf" ALTER COLUMN "returnType" TYPE VARCHAR(255);
ALTER TABLE "cluster" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "service" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "service_configuration" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "service_configuration" ALTER COLUMN "filename" TYPE VARCHAR(255);
ALTER TABLE "component" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "service_bundle" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "role" ALTER COLUMN "displayName" TYPE VARCHAR(255);
ALTER TABLE "topology_test_run_case" ALTER COLUMN "name" TYPE VARCHAR(255);
ALTER TABLE "topology_test_run_histories" ALTER COLUMN "eventLogFilePath" TYPE VARCHAR(255);