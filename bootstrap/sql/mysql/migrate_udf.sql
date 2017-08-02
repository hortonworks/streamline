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