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

ALTER TABLE "widget_datasource_mapping" DROP CONSTRAINT "widget_datasource_mapping_pkey";
ALTER TABLE "namespace_service_cluster_mapping" DROP CONSTRAINT "namespace_service_cluster_mapping_pkey";
ALTER TABLE "tag_storable_mapping" DROP CONSTRAINT "tag_storable_mapping_pkey";
ALTER TABLE "topology_source_stream_mapping" DROP CONSTRAINT "topology_source_stream_mapping_pkey";
ALTER TABLE "topology_processor_stream_mapping" DROP CONSTRAINT "topology_processor_stream_mapping_pkey";

ALTER TABLE "widget_datasource_mapping" RENAME TO "widget_datasource_map";
ALTER TABLE "namespace_service_cluster_mapping" RENAME TO "namespace_service_cluster_map";
ALTER TABLE "tag_storable_mapping" RENAME TO "tag_storable_map";
ALTER TABLE "topology_source_stream_mapping" RENAME TO "topology_source_stream_map";
ALTER TABLE "topology_processor_stream_mapping" RENAME TO "topology_processor_stream_map";


ALTER TABLE "widget_datasource_map" ADD PRIMARY KEY ("widgetId", "datasourceId");
ALTER TABLE "namespace_service_cluster_map" ADD PRIMARY KEY ("namespaceId", "serviceName", "clusterId");
ALTER TABLE "tag_storable_map" ADD PRIMARY KEY ("tagId", "storableNamespace", "storableId");
ALTER TABLE "topology_source_stream_map" ADD PRIMARY KEY ("sourceId", "versionId", "streamId");
ALTER TABLE "topology_processor_stream_map" ADD PRIMARY KEY ("processorId", "versionId", "streamId");


ALTER TABLE "topology_component_bundle" RENAME COLUMN "topologyComponentUISpecification" TO "topologyComponentUISpec";