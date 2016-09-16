#!/usr/bin/env bash

dir=$(dirname $0)/..

# Load UDF functions
echo "Adding aggregate functions"
echo "stddev"
curl -s -X POST 'http://localhost:8080/api/v1/catalog/streams/udfs' -F udfJarFile=@${dir}/streams/functions/target/streams-functions-0.1.0-SNAPSHOT.jar -F udfConfig='{"name":"STDDEV", "description": "Standard deviation", "type":"AGGREGATE", "className":"com.hortonworks.iotas.streams.udaf.Stddev"};type=application/json'
echo

echo "stddevp"
curl -s -X POST 'http://localhost:8080/api/v1/catalog/streams/udfs' -F udfJarFile=@${dir}/streams/functions/target/streams-functions-0.1.0-SNAPSHOT.jar -F udfConfig='{"name":"STDDEVP", "description": "Population standard deviation", "type":"AGGREGATE", "className":"com.hortonworks.iotas.streams.udaf.Stddevp"};type=application/json'
echo

echo "variance"
curl -s -X POST 'http://localhost:8080/api/v1/catalog/streams/udfs' -F udfJarFile=@${dir}/streams/functions/target/streams-functions-0.1.0-SNAPSHOT.jar -F udfConfig='{"name":"VARIANCE", "description": "Variance", "type":"AGGREGATE", "className":"com.hortonworks.iotas.streams.udaf.Variance"};type=application/json'
echo

echo "variancep"
curl -s -X POST 'http://localhost:8080/api/v1/catalog/streams/udfs' -F udfJarFile=@${dir}/streams/functions/target/streams-functions-0.1.0-SNAPSHOT.jar -F udfConfig='{"name":"VARIANCEP", "description": "Population variance", "type":"AGGREGATE", "className":"com.hortonworks.iotas.streams.udaf.Variancep"};type=application/json'
echo

echo "collectlist"
curl -s -X POST 'http://localhost:8080/api/v1/catalog/streams/udfs' -F udfJarFile=@${dir}/streams/functions/target/streams-functions-0.1.0-SNAPSHOT.jar -F udfConfig='{"name":"COLLECTLIST", "description": "Collect", "type":"AGGREGATE", "className":"com.hortonworks.iotas.streams.udaf.Collectlist"};type=application/json'
echo

echo "topn"
curl -s -X POST 'http://localhost:8080/api/v1/catalog/streams/udfs' -F udfJarFile=@${dir}/streams/functions/target/streams-functions-0.1.0-SNAPSHOT.jar -F udfConfig='{"name":"TOPN", "description": "Top N", "type":"AGGREGATE", "className":"com.hortonworks.iotas.streams.udaf.Topn"};type=application/json'
echo

# Dummy entries for built in functions so that it shows up in the UI
echo "Adding dummy entries for builtin functions"
curl -s -X POST 'http://localhost:8080/api/v1/catalog/streams/udfs' -F udfConfig='{"name":"MIN", "description": "Minimum", "type":"AGGREGATE", "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST 'http://localhost:8080/api/v1/catalog/streams/udfs' -F udfConfig='{"name":"MAX", "description": "Maximum", "type":"AGGREGATE", "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST 'http://localhost:8080/api/v1/catalog/streams/udfs' -F udfConfig='{"name":"SUM", "description": "Sum", "type":"AGGREGATE", "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST 'http://localhost:8080/api/v1/catalog/streams/udfs' -F udfConfig='{"name":"AVG", "description": "Average", "type":"AGGREGATE", "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST 'http://localhost:8080/api/v1/catalog/streams/udfs' -F udfConfig='{"name":"COUNT", "description": "Count", "type":"AGGREGATE", "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST 'http://localhost:8080/api/v1/catalog/streams/udfs' -F udfConfig='{"name":"UPPER", "description": "Uppercase", "type":"FUNCTION", "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST 'http://localhost:8080/api/v1/catalog/streams/udfs' -F udfConfig='{"name":"LOWER", "description": "Lowercase", "type":"FUNCTION", "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST 'http://localhost:8080/api/v1/catalog/streams/udfs' -F udfConfig='{"name":"INITCAP", "description": "First letter of each word in uppercase", "type":"FUNCTION", "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST 'http://localhost:8080/api/v1/catalog/streams/udfs' -F udfConfig='{"name":"SUBSTRING", "description": "Substring", "type":"FUNCTION", "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST 'http://localhost:8080/api/v1/catalog/streams/udfs' -F udfConfig='{"name":"CHAR_LENGTH", "description": "String length", "type":"FUNCTION", "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST 'http://localhost:8080/api/v1/catalog/streams/udfs' -F udfConfig='{"name":"CONCAT", "description": "Concatenate", "type":"FUNCTION", "className":"builtin"};type=application/json' -F builtin=true
echo
echo "Done"
