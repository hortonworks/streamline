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


# defaults
verbose=false
shell_dir=$(dirname $0)
bootstrap_dir=${shell_dir}/..
CONFIG_FILE_PATH=${bootstrap_dir}/../conf/streamline.yaml

# Which java to use
if [ -z "${JAVA_HOME}" ]; then
  JAVA="java"
else
  JAVA="${JAVA_HOME}/bin/java"
fi

function run_cmd {
  cmd=$*
  if [[ $verbose == "true" ]]
  then
    echo $cmd
  fi
  response=$(eval $cmd)

  if [ $? -ne 0 ] ; then
     echo "Command failed to execute, quiting the migration ..."
     exit 1
  fi

  if [[ $verbose == "true" ]]
  then
    echo $response
  else
    echo $response | grep -o '"responseMessage":[^"]*"[^"]*"'
  fi
  echo "--------------------------------------"
}

function getId {
  str=$1
  echo $str | grep -o -E "\"id\":[0-9]+" | head -n1 | cut -d : -f2
}

function getAdminRoleId {
  cmd="curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -sS -X GET ${CATALOG_ROOT_URL}/roles?name=ROLE_ADMIN -H 'Content-Type: application/json'"
  response=$(eval $cmd)
  getId "$response"
}

function put {
  uri=$1/$2
  data=$3
  cmd="curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -sS -X PUT ${CATALOG_ROOT_URL}$uri --data @$data -H 'Content-Type: application/json'"
  echo "PUT $data"
  run_cmd $cmd
}

function post {
  uri=$1
  data=$2
  cmd="curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -sS -X POST ${CATALOG_ROOT_URL}$uri --data @$data -H 'Content-Type: application/json'"
  echo "POST $data"
  run_cmd $cmd
}

function add_sample_topology_component_bundle {
  echo "POST sample_bundle"
  cmd="curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -sS -X POST -i -F topologyComponentBundle=@$bootstrap_dir/kafka-topology-bundle ${CATALOG_ROOT_URL}/streams/componentbundles/SOURCE/"
  run_cmd $cmd
}

function add_topology_component_bundle {
  uri=$1
  data=$2
  cmd="curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -sS -X POST -i -F topologyComponentBundle=@$data ${CATALOG_ROOT_URL}$uri"
  echo "POST $data"
  run_cmd $cmd
}

function put_topology_component_bundle {
  uri=$1
  data=$2
  subType=$3
  out=$(curl -s -X GET -H "Content-Type: application/json" -H "Cache-Control: no-cache" "${CATALOG_ROOT_URL}$uri?subType=${subType}&streamingEngine=STORM")
  bundleId=$(getId $out)
  cmd="curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -sS -X PUT -i -F topologyComponentBundle=@$data ${CATALOG_ROOT_URL}$uri/$bundleId"
  echo "PUT $data"
  run_cmd $cmd
}

function put_service_bundle {
  uri=$1
  data=$2
  cmd="curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -sS -X PUT ${CATALOG_ROOT_URL}$uri --data @$data -H 'Content-Type: application/json'"
  echo "PUT $data"
  run_cmd $cmd

}

#Below command to update storm version will be called by RE script. Need to remove later. Adding now for convenience
update_storm_version_command="$bootstrap_dir/update-storm-version.sh 1.1.0.3.0.0.0-453"
run_cmd $update_storm_version_command

#---------------------------------------------
# Get catalogRootUrl from configuration file
#---------------------------------------------

CONF_READER_MAIN_CLASS=com.hortonworks.registries.storage.tool.sql.PropertiesReader

for file in "${bootstrap_dir}"/lib/*.jar;
do
    CLASSPATH="$CLASSPATH":"$file"
done

CATALOG_ROOT_URL_PROPERTY_KEY=catalogRootUrl
component_dir=${bootstrap_dir}/components
service_dir=${bootstrap_dir}/services
user_role_dir=${bootstrap_dir}/users_roles

echo "Configuration file: ${CONFIG_FILE_PATH}"

CATALOG_ROOT_URL=`exec ${JAVA} -cp ${CLASSPATH} ${CONF_READER_MAIN_CLASS} ${CONFIG_FILE_PATH} ${CATALOG_ROOT_URL_PROPERTY_KEY}`

# if it doesn't exit with code 0, just give up
if [ $? -ne 0 ]; then
  exit 1
fi

echo "Catalog Root URL: ${CATALOG_ROOT_URL}"
echo "Component bundle Root dir: ${component_dir}"
echo "Service bundle Root dir: ${service_dir}"
echo "User/Role bundle Root dir: ${user_role_dir}"

function update_bundles {
    # === Source ===
    # === Processor ===
    add_topology_component_bundle /streams/componentbundles/PROCESSOR ${component_dir}/processors/v001__rtjoin-bolt-topology-component.json
    # === Sink ===
    put_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/v002__hdfs-sink-topology-component.json HDFS
    put_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/v002__jdbc-sink-topology-component.json JDBC
    put_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/v002__hive-sink-topology-component.json HIVE
    # === Topology ===
    # === Service Bundle ===
    put_service_bundle /servicebundles/KAFKA ${service_dir}/v002__kafka-bundle.json
    put_service_bundle /servicebundles/STORM ${service_dir}/v002__storm-bundle.json
    put_service_bundle /servicebundles/ZOOKEEPER ${service_dir}/v002__zookeeper-bundle.json
    post /servicebundles ${service_dir}/v001__druid-bundle.json
}

function add_udfs {
        dir=$(dirname $0)/../..

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

        echo "  - variance"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt  -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"VARIANCE_FN", "displayName": "VARIANCE", "description": "Variance", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Variance", "builtin":true};type=application/json'

        echo "  - variancep"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"VARIANCEP_FN", "displayName": "VARIANCEP", "description": "Population variance", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Variancep", "builtin":true};type=application/json'

        echo "  - stddev"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"STDDEV_FN", "displayName": "STDDEV", "description": "Standard deviation", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Stddev", "builtin":true};type=application/json'

        echo "  - stddevp"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt  -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"STDDEVP_FN", "displayName": "STDDEVP", "description": "Population standard deviation", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Stddevp", "builtin":true};type=application/json'

        echo "  - concat"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"CONCAT_FN", "displayName": "CONCAT", "description": "Concatenate", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Concat", "builtin":true};type=application/json'

        echo "  - count"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"COUNT_FN", "displayName": "COUNT","description": "Count", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.LongCount", "builtin":true};type=application/json'

        echo "  - substring"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"SUBSTRING_FN", "displayName": "SUBSTRING", "description": "Returns sub-string of a string starting at some position", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Substring", "builtin":true};type=application/json'

        echo "  - substring"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"SUBSTRING_FN", "displayName": "SUBSTRING", "description": "Returns a sub-string of a string starting at some position and is of given length", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Substring2", "builtin":true};type=application/json'

        echo "  - position"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"POSITION_FN", "displayName": "POSITION", "description": "Returns the position of the first occurrence of sub-string in  a string", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Position", "builtin":true};type=application/json'

        echo "  - position"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"POSITION_FN", "displayName": "POSITION", "description": "Returns the position of the first occurrence of sub-string in  a string starting the search from an index", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Position2", "builtin":true};type=application/json'

        echo "  - avg"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"AVG_FN", "displayName": "AVG","description": "Average", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Mean", "builtin":true};type=application/json'

        echo "  - trim"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"TRIM_FN", "displayName": "TRIM", "description": "Returns a string with any leading and trailing whitespaces removed", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Trim", "builtin":true};type=application/json'

        echo "  - trim2"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"TRIM_FN", "displayName": "TRIM2", "description": "Returns a string with specified leading and trailing character removed", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Trim2", "builtin":true};type=application/json'

        echo "  - ltrim"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"LTRIM_FN", "displayName": "LTRIM", "description": "Removes leading whitespaces from the input", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Ltrim", "builtin":true};type=application/json'

        echo "  - ltrim2"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"LTRIM_FN", "displayName": "LTRIM", "description": "Removes specified leading character from the input", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Ltrim2", "builtin":true};type=application/json'

        echo "  - rtrim"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"RTRIM_FN", "displayName": "RTRIM", "description": "Removes trailing whitespaces from the input", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Rtrim", "builtin":true};type=application/json'

        echo "  - rtrim2"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"RTRIM_FN", "displayName": "RTRIM", "description": "Removes specified trailing character from the input", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Rtrim2", "builtin":true};type=application/json'

        echo "  - overlay"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"OVERLAY_FN", "displayName": "OVERLAY", "description": "Replaces a substring of a string with a replacement string", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Overlay", "builtin":true};type=application/json'

        echo "  - overlay"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"OVERLAY_FN", "displayName": "OVERLAY", "description": "Replaces a substring of a string with a replacement string", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Overlay2", "builtin":true};type=application/json'

        echo "  - sum"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"SUM_FN", "displayName": "SUM","description": "Sum", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.NumberSum", "builtin":true};type=application/json'

        # Dummy entries for built in functions so that it shows up in the UI
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
}

function main {
    echo ""
    echo "===================================================================================="
    echo "Running bootstrap.sh will create streamline default components, notifiers, udfs and roles"

    update_bundles
    add_udfs
}

main