#!/usr/bin/env bash
#
# Copyright 2017 Hortonworks.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

#   http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

dir=$(dirname $0)/..
bootstrap_dir=$(dirname $0)
CATALOG_ROOT_URL="${1:-http://localhost:8080/api/v1/catalog}"

jarFile="$(find ${bootstrap_dir}/udf-jars/ -name 'streamline-functions-*.jar')"
if [[ ! -f ${jarFile} ]]
then
  # try local build path
  jarFile="$(find ${dir}/streams/functions/target/ -name 'streamline-functions-*.jar')"
  if [[ ! -f ${jarFile} ]]
  then
    echo "Could not find streamline-functions jar, Exiting ..."
    exit 1
  fi
fi

# Load UDF functions
echo "Adding aggregate functions"
echo "  - stddev"
curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"STDDEV", "displayName": "STDDEV", "description": "Standard deviation", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Stddev"};type=application/json'

echo "  - stddevp"
curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"STDDEVP", "displayName": "STDDEVP", "description": "Population standard deviation", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Stddevp"};type=application/json'

echo "  - variance"
curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"VARIANCE", "displayName": "VARIANCE", "description": "Variance", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Variance"};type=application/json'

echo "  - variancep"
curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"VARIANCEP", "displayName": "VARIANCEP", "description": "Population variance", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Variancep"};type=application/json'

echo "  - avg"
curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"MEAN", "displayName": "AVG","description": "Average", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Mean"};type=application/json'

# TODO: Code generation issues in calcite code generator. See https://github.com/hortonworks/streamline/pull/422#issuecomment-270330293
#echo "  - collectlist"
#curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"COLLECTLIST", "displayName": "COLLECTLIST", "description": "Collect", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.CollectList"};type=application/json'

# TODO: Code generation issues in calcite code generator. See https://github.com/hortonworks/streamline/pull/422#issuecomment-270330293
#echo "  - topn"
#curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"TOPN", "displayName": "TOPN", "description": "Top N", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Topn"};type=application/json'

echo "  - identity"
curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"IDENTITY", "displayName": "Identity", "description": "Identity function", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Identity"};type=application/json'

# Dummy entries for built in functions so that it shows up in the UI
echo "Adding builtin functions"
curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"MIN", "displayName": "MIN", "description": "Minimum", "type":"AGGREGATE", "argTypes":["BOOLEAN|BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE|STRING"], "className":"builtin"};type=application/json' -F builtin=true
curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"MAX", "displayName": "MAX", "description": "Maximum", "type":"AGGREGATE", "argTypes":["BOOLEAN|BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE|STRING"], "className":"builtin"};type=application/json' -F builtin=true
curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"SUM", "displayName": "SUM", "description": "Sum", "type":"AGGREGATE", "argTypes":["BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE"], "className":"builtin"};type=application/json' -F builtin=true
curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"COUNT", "displayName": "COUNT", "description": "Count", "type":"AGGREGATE", "argTypes":["BOOLEAN|BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE|STRING|BINARY|NESTED|ARRAY"], "returnType": "LONG", "className":"builtin"};type=application/json' -F builtin=true
curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"UPPER", "displayName": "UPPER", "description": "Uppercase", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin"};type=application/json' -F builtin=true
curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"LOWER", "displayName": "LOWER", "description": "Lowercase", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin"};type=application/json' -F builtin=true
curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"INITCAP", "displayName": "INITCAP", "description": "First letter of each word in uppercase", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin"};type=application/json' -F builtin=true
curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"SUBSTRING", "displayName": "SUBSTRING", "description": "Substring", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin"};type=application/json' -F builtin=true
curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"CHAR_LENGTH", "displayName": "CHAR_LENGTH", "description": "String length", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "LONG", "className":"builtin"};type=application/json' -F builtin=true
curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"CONCAT", "displayName": "CONCAT", "description": "Concatenate", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin"};type=application/json' -F builtin=true
