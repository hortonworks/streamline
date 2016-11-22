#!/usr/bin/env bash

dir=$(dirname $0)/..
host="${1:-localhost}"
port="${2:-8080}"

jarFile=./udf-jars/streamline-functions-0.1.0-SNAPSHOT.jar
if [[ ! -f ${jarFile} ]]
then
  # try local build path
  jarFile=${dir}/streams/functions/target/streamline-functions-0.1.0-SNAPSHOT.jar
  if [[ ! -f ${jarFile} ]]
  then
    echo "Could not find streamline-functions jar, Exiting ..."
    exit 1
  fi
fi

# Load UDF functions
echo "Adding aggregate functions"
echo "stddev"
curl -s -X POST "http://${host}:${port}/api/v1/catalog/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"STDDEV", "displayName": "STDDEV", "description": "Standard deviation", "type":"AGGREGATE", "className":"org.apache.streamline.streams.udaf.Stddev"};type=application/json'
echo

echo "stddevp"
curl -s -X POST "http://${host}:${port}/api/v1/catalog/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"STDDEVP", "displayName": "STDDEVP", "description": "Population standard deviation", "type":"AGGREGATE", "className":"org.apache.streamline.streams.udaf.Stddevp"};type=application/json'
echo

echo "variance"
curl -s -X POST "http://${host}:${port}/api/v1/catalog/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"VARIANCE", "displayName": "VARIANCE", "description": "Variance", "type":"AGGREGATE", "className":"org.apache.streamline.streams.udaf.Variance"};type=application/json'
echo

echo "variancep"
curl -s -X POST "http://${host}:${port}/api/v1/catalog/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"VARIANCEP", "displayName": "VARIANCEP", "description": "Population variance", "type":"AGGREGATE", "className":"org.apache.streamline.streams.udaf.Variancep"};type=application/json'
echo

echo "avg"
curl -s -X POST "http://${host}:${port}/api/v1/catalog/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"MEAN", "displayName": "AVG","description": "Average", "type":"AGGREGATE", "className":"org.apache.streamline.streams.udaf.Mean"};type=application/json'
echo

echo "sum"
curl -s -X POST "http://${host}:${port}/api/v1/catalog/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"NUMBERSUM", "displayName": "SUM","description": "Sum", "type":"AGGREGATE", "className":"org.apache.streamline.streams.udaf.NumberSum"};type=application/json'
echo

echo "collectlist"
curl -s -X POST "http://${host}:${port}/api/v1/catalog/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"COLLECTLIST", "displayName": "COLLECTLIST", "description": "Collect", "type":"AGGREGATE", "className":"org.apache.streamline.streams.udaf.CollectList"};type=application/json'
echo

echo "topn"
curl -s -X POST "http://${host}:${port}/api/v1/catalog/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"TOPN", "displayName": "TOPN", "description": "Top N", "type":"AGGREGATE", "className":"org.apache.streamline.streams.udaf.Topn"};type=application/json'
echo

# Dummy entries for built in functions so that it shows up in the UI
echo "Adding dummy entries for builtin functions"
curl -s -X POST "http://${host}:${port}/api/v1/catalog/streams/udfs" -F udfConfig='{"name":"MIN", "displayName": "MIN", "description": "Minimum", "type":"AGGREGATE", "argTypes":["BOOLEAN|BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE|STRING"], "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST "http://${host}:${port}/api/v1/catalog/streams/udfs" -F udfConfig='{"name":"MAX", "displayName": "MAX", "description": "Maximum", "type":"AGGREGATE", "argTypes":["BOOLEAN|BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE|STRING"], "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST "http://${host}:${port}/api/v1/catalog/streams/udfs" -F udfConfig='{"name":"COUNT", "displayName": "COUNT", "description": "Count", "type":"AGGREGATE", "argTypes":["BOOLEAN|BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE|STRING|BINARY|NESTED|ARRAY"], "returnType": "LONG", "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST "http://${host}:${port}/api/v1/catalog/streams/udfs" -F udfConfig='{"name":"UPPER", "displayName": "UPPER", "description": "Uppercase", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST "http://${host}:${port}/api/v1/catalog/streams/udfs" -F udfConfig='{"name":"LOWER", "displayName": "LOWER", "description": "Lowercase", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST "http://${host}:${port}/api/v1/catalog/streams/udfs" -F udfConfig='{"name":"INITCAP", "displayName": "INITCAP", "description": "First letter of each word in uppercase", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST "http://${host}:${port}/api/v1/catalog/streams/udfs" -F udfConfig='{"name":"SUBSTRING", "displayName": "SUBSTRING", "description": "Substring", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST "http://${host}:${port}/api/v1/catalog/streams/udfs" -F udfConfig='{"name":"CHAR_LENGTH", "displayName": "CHAR_LENGTH", "description": "String length", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "LONG", "className":"builtin"};type=application/json' -F builtin=true
echo
curl -s -X POST "http://${host}:${port}/api/v1/catalog/streams/udfs" -F udfConfig='{"name":"CONCAT", "displayName": "CONCAT", "description": "Concatenate", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin"};type=application/json' -F builtin=true
echo
echo "Done"
