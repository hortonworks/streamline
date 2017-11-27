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

update topology_window set parsedRuleStr=REPLACE(parsedRuleStr, '"STDDEV"', '"STDDEV_FN"'), projections=REPLACE(projections, '"STDDEV"', '"STDDEV_FN"');
update topology_window set parsedRuleStr=REPLACE(parsedRuleStr, '"STDDEVP"', '"STDDEVP_FN"'), projections=REPLACE(projections, '"STDDEVP"', '"STDDEVP_FN"');
update topology_window set parsedRuleStr=REPLACE(parsedRuleStr, '"VARIANCE"', '"VARIANCE_FN"'), projections=REPLACE(projections, '"VARIANCE"', '"VARIANCE_FN"');
update topology_window set parsedRuleStr=REPLACE(parsedRuleStr, '"VARIANCEP"', '"VARIANCEP_FN"'), projections=REPLACE(projections, '"VARIANCEP"', '"VARIANCEP_FN"');
update topology_window set parsedRuleStr=REPLACE(parsedRuleStr, '"MEAN"', '"AVG_FN"'), projections=REPLACE(projections, '"MEAN"', '"AVG_FN"');
update topology_window set parsedRuleStr=REPLACE(parsedRuleStr, '"NUMBERSUM"', '"SUM_FN"'), projections=REPLACE(projections, '"NUMBERSUM"', '"SUM_FN"');
update topology_window set parsedRuleStr=REPLACE(parsedRuleStr, '"LONGCOUNT"', '"COUNT_FN"'), projections=REPLACE(projections, '"LONGCOUNT"', '"COUNT_FN"');

update topology_rule set parsedRuleStr=REPLACE(parsedRuleStr, '"CONCAT"', '"CONCAT_FN"'), projections=REPLACE(projections, '"CONCAT"', '"CONCAT_FN"'), `sql`=REPLACE(`sql`, 'CONCAT', 'CONCAT_FN');

delete from udf where name='STDDEV';
delete from udf where name='STDDEVP';
delete from udf where name='VARIANCE';
delete from udf where name='VARIANCEP';
delete from udf where name='MEAN';
delete from udf where name='NUMBERSUM';
delete from udf where name='LONGCOUNT';
delete from udf where name='CONCAT';