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

ALTER TABLE "service" ADD CONSTRAINT "service_fk_cluster" FOREIGN KEY ("clusterId") REFERENCES "cluster" ("id");
ALTER TABLE "service_configuration" ADD CONSTRAINT service_configuration_fk_srv FOREIGN KEY ("serviceId") REFERENCES "service" ("id");
ALTER TABLE "component" ADD CONSTRAINT component_fk_service FOREIGN KEY ("serviceId") REFERENCES "service" ("id");