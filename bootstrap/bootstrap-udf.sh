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
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"STDDEV_FN", "displayName": "STDDEV", "description": "Standard deviation", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Stddev", "builtin":true};type=application/json'

echo "  - stddevp"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt  -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"STDDEVP_FN", "displayName": "STDDEVP", "description": "Population standard deviation", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Stddevp", "builtin":true};type=application/json'

echo "  - variance"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt  -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"VARIANCE_FN", "displayName": "VARIANCE", "description": "Variance", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Variance", "builtin":true};type=application/json'

echo "  - variancep"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"VARIANCEP_FN", "displayName": "VARIANCEP", "description": "Population variance", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Variancep", "builtin":true};type=application/json'

echo "  - avg"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"AVG_FN", "displayName": "AVG","description": "Average", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Mean", "builtin":true};type=application/json'

echo "  - sum"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"SUM_FN", "displayName": "SUM","description": "Sum", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.NumberSum", "builtin":true};type=application/json'

echo "  - count"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"COUNT_FN", "displayName": "COUNT","description": "Count", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.LongCount", "builtin":true};type=application/json'

# TODO: Code generation issues in calcite code generator. See https://github.com/hortonworks/streamline/pull/422#issuecomment-270330293
#echo "  - collectlist"
#curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"COLLECTLIST", "displayName": "COLLECTLIST", "description": "Collect", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.CollectList", "builtin":true};type=application/json'

# TODO: Code generation issues in calcite code generator. See https://github.com/hortonworks/streamline/pull/422#issuecomment-270330293
#echo "  - topn"
#curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"TOPN", "displayName": "TOPN", "description": "Top N", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Topn", "builtin":true};type=application/json'

echo "  - identity"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"IDENTITY_FN", "displayName": "IDENTITY", "description": "Identity function", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Identity", "builtin":true};type=application/json'

echo "  - concat"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"CONCAT_FN", "displayName": "CONCAT", "description": "Concatenate", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Concat", "builtin":true};type=application/json'

echo "  - substring"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"SUBSTRING_FN", "displayName": "SUBSTRING", "description": "Returns sub-string of a string starting at some position", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Substring", "builtin":true};type=application/json'

echo "  - substring"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"SUBSTRING_FN", "displayName": "SUBSTRING", "description": "Returns a sub-string of a string starting at some position and is of given length", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Substring2", "builtin":true};type=application/json'

echo "  - position"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"POSITION_FN", "displayName": "POSITION", "description": "Returns the position of the first occurrence of sub-string in  a string", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Position", "builtin":true};type=application/json'

echo "  - position"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"POSITION_FN", "displayName": "POSITION", "description": "Returns the position of the first occurrence of sub-string in  a string starting the search from an index", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Position2", "builtin":true};type=application/json'

echo "  - trim"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"TRIM_FN", "displayName": "TRIM", "description": "Returns a string with any leading and trailing whitespaces removed", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Trim", "builtin":true};type=application/json'

echo "  - trim2"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"TRIM2_FN", "displayName": "TRIM2", "description": "Returns a string with specified leading and trailing character removed", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Trim2", "builtin":true};type=application/json'

echo "  - ltrim"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"LTRIM_FN", "displayName": "LTRIM", "description": "Removes leading whitespaces from the input", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Ltrim", "builtin":true};type=application/json'

echo "  - ltrim2"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"LTRIM2_FN", "displayName": "LTRIM", "description": "Removes specified leading character from the input", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Ltrim2", "builtin":true};type=application/json'

echo "  - rtrim"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"RTRIM_FN", "displayName": "RTRIM", "description": "Removes trailing whitespaces from the input", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Rtrim", "builtin":true};type=application/json'

echo "  - rtrim2"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"RTRIM2_FN", "displayName": "RTRIM", "description": "Removes specified trailing character from the input", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Rtrim2", "builtin":true};type=application/json'

echo "  - overlay"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"OVERLAY_FN", "displayName": "OVERLAY", "description": "Replaces a substring of a string with a replacement string", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Overlay", "builtin":true};type=application/json'

echo "  - overlay"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"OVERLAY_FN", "displayName": "OVERLAY", "description": "Replaces a substring of a string with a replacement string", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Overlay2", "builtin":true};type=application/json'


# Dummy entries for built in functions so that it shows up in the UI
echo "Adding builtin functions"
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt  -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"MIN", "displayName": "MIN", "description": "Minimum", "type":"AGGREGATE", "argTypes":["BOOLEAN|BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE|STRING"], "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"MAX", "displayName": "MAX", "description": "Maximum", "type":"AGGREGATE", "argTypes":["BOOLEAN|BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE|STRING"], "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"UPPER", "displayName": "UPPER", "description": "Uppercase", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"LOWER", "displayName": "LOWER", "description": "Lowercase", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"INITCAP", "displayName": "INITCAP", "description": "First letter of each word in uppercase", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
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