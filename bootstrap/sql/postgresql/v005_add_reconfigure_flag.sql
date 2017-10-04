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

-- Add reconfigure flag to components
ALTER TABLE "topology_source" ADD COLUMN "reconfigure" BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE "topology_processor" ADD COLUMN "reconfigure" BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE "topology_sink" ADD COLUMN "reconfigure" BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE "topology_rule" ADD COLUMN "reconfigure" BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE "topology_branchrule" ADD COLUMN "reconfigure" BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE "topology_window" ADD COLUMN "reconfigure" BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE "topology_component" ADD COLUMN "reconfigure" BOOLEAN NOT NULL DEFAULT false;
