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

ALTER TABLE `dashboard` MODIFY `description` VARCHAR(255) NOT NULL;
ALTER TABLE `ml_model` MODIFY `uploadedFileName` VARCHAR(255) NOT NULL;
ALTER TABLE `widget` MODIFY `description` VARCHAR(255) NOT NULL;
ALTER TABLE `widget` MODIFY `type` VARCHAR(255) NOT NULL;
ALTER TABLE `datasource` MODIFY `description` VARCHAR(255) NOT NULL;
ALTER TABLE `datasource` MODIFY `type` VARCHAR(255) NOT NULL;
ALTER TABLE `datasource` MODIFY `url` VARCHAR(255) NOT NULL;
ALTER TABLE `namespace` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `namespace` MODIFY `streamingEngine` VARCHAR(255) NOT NULL;
ALTER TABLE `namespace` MODIFY `timeSeriesDB` VARCHAR(255) NULL;
ALTER TABLE `namespace` MODIFY `description` VARCHAR(255);
ALTER TABLE `topology_version` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `topology` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `topology_component_bundle` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `notifier` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `topology_component` MODIFY `name` VARCHAR(255);
ALTER TABLE `topology_source` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `topology_sink` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `topology_processor` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `topology_rule` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `topology_branchrule` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `topology_window` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `udf` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `udf` MODIFY `displayName` VARCHAR(255) NOT NULL;
ALTER TABLE `udf` MODIFY `type` VARCHAR(255) NOT NULL;
ALTER TABLE `udf` MODIFY `className` VARCHAR(255) NOT NULL;
ALTER TABLE `udf` MODIFY `jarStoragePath` VARCHAR(255) NOT NULL;
ALTER TABLE `udf` MODIFY `digest` VARCHAR(255) NOT NULL;
ALTER TABLE `udf` MODIFY `argTypes` VARCHAR(255) NOT NULL;
ALTER TABLE `udf` MODIFY `returnType` VARCHAR(255) NOT NULL;
ALTER TABLE `cluster` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `service` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `service_configuration` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `service_configuration` MODIFY `filename` VARCHAR(255);
ALTER TABLE `component` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `service_bundle` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `topology_test_run_case` MODIFY `name` VARCHAR(255) NOT NULL;
ALTER TABLE `topology_test_run_histories` MODIFY `eventLogFilePath` VARCHAR(255) NOT NULL;


ALTER TABLE `topology_test_run_case_source` MODIFY `records` MEDIUMTEXT NOT NULL;
ALTER TABLE `topology_test_run_case_sink` MODIFY `records` MEDIUMTEXT NOT NULL;
ALTER TABLE `topology_test_run_histories` MODIFY `expectedOutputRecords` MEDIUMTEXT;
ALTER TABLE `topology_test_run_histories` MODIFY `actualOutputRecords` MEDIUMTEXT;