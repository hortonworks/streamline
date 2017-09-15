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

-- Shorten the variable length inorder to support Oracle 11gr2 where maximum length is 30 characters


RENAME TABLE `widget_datasource_mapping` TO `widget_datasource_map`;
RENAME TABLE `namespace_service_cluster_mapping` TO `namespace_service_cluster_map`;
RENAME TABLE `tag_storable_mapping` TO `tag_storable_map`;
RENAME TABLE `topology_source_stream_mapping` TO `topology_source_stream_map`;
RENAME TABLE `topology_processor_stream_mapping` TO `topology_processor_stream_map`;

ALTER TABLE `topology_component_bundle` CHANGE COLUMN `topologyComponentUISpecification` `topologyComponentUISpec` TEXT NOT NULL;
