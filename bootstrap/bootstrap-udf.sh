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
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"STDDEV", "displayName": "STDDEV", "description": "Standard deviation", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Stddev", "builtin":true};type=application/json'

echo "  - stddevp"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt  -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"STDDEVP", "displayName": "STDDEVP", "description": "Population standard deviation", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Stddevp", "builtin":true};type=application/json'

echo "  - variance"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt  -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"VARIANCE", "displayName": "VARIANCE", "description": "Variance", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Variance", "builtin":true};type=application/json'

echo "  - variancep"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"VARIANCEP", "displayName": "VARIANCEP", "description": "Population variance", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Variancep", "builtin":true};type=application/json'

echo "  - avg"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"MEAN", "displayName": "AVG","description": "Average", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Mean", "builtin":true};type=application/json'

echo "  - sum"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"NUMBERSUM", "displayName": "SUM","description": "Sum", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.NumberSum", "builtin":true};type=application/json'

echo "  - count"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"LONGCOUNT", "displayName": "COUNT","description": "Count", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.LongCount", "builtin":true};type=application/json'

# TODO: Code generation issues in calcite code generator. See https://github.com/hortonworks/streamline/pull/422#issuecomment-270330293
#echo "  - collectlist"
#curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"COLLECTLIST", "displayName": "COLLECTLIST", "description": "Collect", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.CollectList", "builtin":true};type=application/json'

# TODO: Code generation issues in calcite code generator. See https://github.com/hortonworks/streamline/pull/422#issuecomment-270330293
#echo "  - topn"
#curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"TOPN", "displayName": "TOPN", "description": "Top N", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Topn", "builtin":true};type=application/json'

echo "  - identity"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"IDENTITY_FN", "displayName": "IDENTITY", "description": "Identity function", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Identity", "builtin":true};type=application/json'

echo "  - concat"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"CONCAT", "displayName": "CONCAT", "description": "Concatenate", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Concat", "builtin":true};type=application/json'

# Dummy entries for built in functions so that it shows up in the UI
echo "Adding builtin functions"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt  -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"MIN", "displayName": "MIN", "description": "Minimum", "type":"AGGREGATE", "argTypes":["BOOLEAN|BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE|STRING"], "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"MAX", "displayName": "MAX", "description": "Maximum", "type":"AGGREGATE", "argTypes":["BOOLEAN|BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE|STRING"], "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"UPPER", "displayName": "UPPER", "description": "Uppercase", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"LOWER", "displayName": "LOWER", "description": "Lowercase", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"INITCAP", "displayName": "INITCAP", "description": "First letter of each word in uppercase", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
#curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"SUBSTRING", "displayName": "SUBSTRING", "description": "Substring", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"CHAR_LENGTH", "displayName": "CHAR_LENGTH", "description": "String length", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "LONG", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"POWER", "displayName": "POWER", "description": "First argument raised to the power of the second argument", "type":"FUNCTION", "argTypes":["BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE", "BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"ABS", "displayName": "ABS", "description": "Returns the absolute value", "type":"FUNCTION", "argTypes":["BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE"], "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"MOD", "displayName": "MOD", "description": "Returns the remainder", "type":"FUNCTION", "argTypes":["BYTE|SHORT|INTEGER|LONG", "BYTE|SHORT|INTEGER|LONG"], "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"SQRT", "displayName": "SQRT", "description": "Returns the square root", "type":"FUNCTION", "argTypes":["BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"LN", "displayName": "LN", "description": "Returns the natural logarithm", "type":"FUNCTION", "argTypes":["BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"LOG10", "displayName": "LOG10", "description": "Returns the base 10 logarithm", "type":"FUNCTION", "argTypes":["BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"EXP", "displayName": "EXP", "description": "Returns e raised to the power of the argument", "type":"FUNCTION", "argTypes":["BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"CEIL", "displayName": "CEIL", "description": "Rounds up, returning the smallest integer that is greater than or equal to the argument", "type":"FUNCTION", "argTypes":["FLOAT|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"FLOOR", "displayName": "FLOOR", "description": "Rounds down, returning the largest integer that is less than or equal to the argument", "type":"FUNCTION", "argTypes":["FLOAT|DOUBLE"], "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"RAND", "displayName": "RAND", "description": "Generates a random double between 0 and 1 (inclusive)", "type":"FUNCTION", "returnType": "DOUBLE", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"RAND_INTEGER", "displayName": "RAND_INTEGER", "description": "Generates a random integer between 0 and the argument (exclusive)", "type":"FUNCTION", "argTypes":["BYTE|SHORT|INTEGER|LONG"], "returnType": "INTEGER", "className":"builtin", "builtin":true};type=application/json' -F builtin=true