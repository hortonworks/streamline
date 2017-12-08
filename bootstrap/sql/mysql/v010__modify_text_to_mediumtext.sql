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


-- Change column type from TEXT to MEDIUMTEXT

ALTER TABLE `topology_component_bundle` MODIFY `topologyComponentUISpec` MEDIUMTEXT NOT NULL;
ALTER TABLE `topology_stream` MODIFY `fieldsData` MEDIUMTEXT NOT NULL;
ALTER TABLE `topology_component` MODIFY `configData` MEDIUMTEXT;
ALTER TABLE `topology_source` MODIFY `configData` MEDIUMTEXT NOT NULL;
ALTER TABLE `topology_sink` MODIFY `configData` MEDIUMTEXT NOT NULL;
ALTER TABLE `topology_processor` MODIFY `configData` MEDIUMTEXT NOT NULL;
ALTER TABLE `topology_edge` MODIFY `streamGroupingsData` MEDIUMTEXT NOT NULL;
ALTER TABLE `service_configuration` MODIFY `configuration` MEDIUMTEXT NOT NULL;
ALTER TABLE `topology_editor_toolbar` MODIFY `data` MEDIUMTEXT NOT NULL;
ALTER TABLE `service_bundle` MODIFY `serviceUISpecification` MEDIUMTEXT NOT NULL;
ALTER TABLE `ml_model` MODIFY `pmml` MEDIUMTEXT NOT NULL;
ALTER TABLE `dashboard` MODIFY `data` MEDIUMTEXT NOT NULL;
ALTER TABLE `widget` MODIFY `data` MEDIUMTEXT NOT NULL;
ALTER TABLE `datasource` MODIFY `data` MEDIUMTEXT NOT NULL;
ALTER TABLE `topology` MODIFY `config` MEDIUMTEXT NOT NULL;
ALTER TABLE `topology_editor_metadata` MODIFY `data` MEDIUMTEXT NOT NULL;

ALTER TABLE `topology_rule`
MODIFY `streams` MEDIUMTEXT NULL,
MODIFY `outputStreams` MEDIUMTEXT NULL,
MODIFY `condition` MEDIUMTEXT NULL,
MODIFY `sql` MEDIUMTEXT NULL,
MODIFY `parsedRuleStr` MEDIUMTEXT NOT NULL,
MODIFY `projections` MEDIUMTEXT NOT NULL,
MODIFY `window` MEDIUMTEXT NOT NULL,
MODIFY `actions` MEDIUMTEXT NOT NULL;

ALTER TABLE `topology_branchrule`
MODIFY `stream` MEDIUMTEXT NOT NULL,
MODIFY `outputStreams` MEDIUMTEXT NULL,
MODIFY `condition` MEDIUMTEXT NOT NULL,
MODIFY `parsedRuleStr` MEDIUMTEXT NOT NULL,
MODIFY `actions` MEDIUMTEXT NOT NULL;

ALTER TABLE `topology_window`
MODIFY `streams` MEDIUMTEXT NULL,
MODIFY `outputStreams` MEDIUMTEXT NULL,
MODIFY `condition` MEDIUMTEXT NULL,
MODIFY `parsedRuleStr` MEDIUMTEXT NOT NULL,
MODIFY `window` MEDIUMTEXT NOT NULL,
MODIFY `actions` MEDIUMTEXT NOT NULL,
MODIFY `projections` MEDIUMTEXT NULL,
MODIFY `groupbykeys` MEDIUMTEXT NULL;


ALTER TABLE `notifier`
MODIFY `properties` MEDIUMTEXT,
MODIFY `fieldValues` MEDIUMTEXT;

