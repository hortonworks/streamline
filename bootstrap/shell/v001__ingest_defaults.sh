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

function add_all_bundles {
    # === Source ===
    add_topology_component_bundle /streams/componentbundles/SOURCE ${component_dir}/sources/v001__kafka-source-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SOURCE ${component_dir}/sources/v001__hdfs-source-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SOURCE ${component_dir}/sources/v001__eventhubs-source-topology-component.json
    # === Processor ===
    add_topology_component_bundle /streams/componentbundles/PROCESSOR ${component_dir}/processors/v001__rule-topology-component.json
    add_topology_component_bundle /streams/componentbundles/PROCESSOR ${component_dir}/processors/v001__window-topology-component.json
    add_topology_component_bundle /streams/componentbundles/PROCESSOR ${component_dir}/processors/v001__branch-topology-component.json
    add_topology_component_bundle /streams/componentbundles/PROCESSOR ${component_dir}/processors/v001__join-bolt-topology-component.json
    add_topology_component_bundle /streams/componentbundles/PROCESSOR ${component_dir}/processors/v001__model-topology-component.json
    add_topology_component_bundle /streams/componentbundles/PROCESSOR ${component_dir}/processors/v001__projection-topology-component.json
    # === Sink ===
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/v001__hdfs-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/v001__hbase-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/v001__notification-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/v001__opentsdb-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/v001__jdbc-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/v001__cassandra-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/v001__druid-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/v001__solr-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/v001__kafka-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/v001__hive-sink-topology-component.json
    # === Topology ===
    add_topology_component_bundle /streams/componentbundles/TOPOLOGY ${component_dir}/topology/v001__storm-topology-component.json

    #add_topology_component_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/v001__split-topology-component
    #add_topology_component_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/v001__normalization-processor-topology-component.json
    #add_topology_component_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/v001__multilang-topology-component.json
    #post /streams/componentbundles/PROCESSOR $component_dir/sinks/v001__stage-topology-component
    #post /streams/componentbundles/ACTION $component_dir/sinks/v001__transform-action-topology-component
    #post /streams/componentbundles/TRANSFORM $component_dir/sinks/v001__projection-transform-topology-component
    #post /streams/componentbundles/TRANSFORM $component_dir/sinks/v001__enrichment-transform-topology-component
    #note that the below is just a sample for ui to work with. Once UI is ready, all above will be replaced with new bundle components
    #add_sample_bundle

    # === service bundles ===
    post /servicebundles ${service_dir}/v001__zookeeper-bundle.json
    post /servicebundles ${service_dir}/v001__storm-bundle.json
    post /servicebundles ${service_dir}/v001__kafka-bundle.json
    post /servicebundles ${service_dir}/v001__hdfs-bundle.json
    post /servicebundles ${service_dir}/v001__hbase-bundle.json
    post /servicebundles ${service_dir}/v001__hive-bundle.json
    post /servicebundles ${service_dir}/v001__email-bundle.json
}

function add_roles_and_users {
    # === anonymous user ===
    post /users ${user_role_dir}/user_anon.json

    # === system roles ===
    for i in ${user_role_dir}/role_*
    do
     if echo $i | grep -E "role_admin$"
     then
        adminId=$(getAdminRoleId)
        if [ -n "$adminId" ]
        then
          echo "Updating admin role, id: $adminId"
          put /roles $adminId $i
          continue
        fi
     fi

     echo "Adding $(basename $i)"
     post /roles $i
    done

    # === role hierarchy  ===
    for i in ${user_role_dir}/children_*
    do
     role_name=$(basename $i | cut -d'_' -f2-)
     echo "Adding child roles for $role_name"
     post /roles/$role_name/children $i
    done
}

function skip_migration_if_not_needed {
    out=$(curl -s -X GET -H "Content-Type: application/json" -H "Cache-Control: no-cache" "${CATALOG_ROOT_URL}/streams/componentbundles/SOURCE?subType=KAFKA")
    bundleId=$(getId $out)
    echo "Existing KAFKA bundle id extracted from SAM : $bundleId"
    if [ "$bundleId" != "" ] ; then
       echo "Skipping migration 1 as the underlying database is upto date"
       exit 0
    fi
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

          # Load UDF functions
        echo "Adding aggregate functions"

        # TODO: Code generation issues in calcite code generator. See https://github.com/hortonworks/streamline/pull/422#issuecomment-270330293
        #echo "  - collectlist"
        #curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"COLLECTLIST", "displayName": "COLLECTLIST", "description": "Collect", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.CollectList", "builtin":true};type=application/json'

        # TODO: Code generation issues in calcite code generator. See https://github.com/hortonworks/streamline/pull/422#issuecomment-270330293
        #echo "  - topn"
        #curl -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"TOPN", "displayName": "TOPN", "description": "Top N", "type":"AGGREGATE", "className":"com.hortonworks.streamline.streams.udaf.Topn", "builtin":true};type=application/json'

        echo "  - identity"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfJarFile=@${jarFile} -F udfConfig='{"name":"IDENTITY_FN", "displayName": "IDENTITY", "description": "Identity function", "type":"FUNCTION", "className":"com.hortonworks.streamline.streams.udf.Identity", "builtin":true};type=application/json'


        # Dummy entries for built in functions so that it shows up in the UI
        echo "Adding builtin functions"
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt  -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"MIN", "displayName": "MIN", "description": "Minimum", "type":"AGGREGATE", "argTypes":["BOOLEAN|BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE|STRING"], "className":"builtin", "builtin":true};type=application/json' -F builtin=true
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"MAX", "displayName": "MAX", "description": "Maximum", "type":"AGGREGATE", "argTypes":["BOOLEAN|BYTE|SHORT|INTEGER|LONG|FLOAT|DOUBLE|STRING"], "className":"builtin", "builtin":true};type=application/json' -F builtin=true
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"UPPER", "displayName": "UPPER", "description": "Uppercase", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"LOWER", "displayName": "LOWER", "description": "Lowercase", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"INITCAP", "displayName": "INITCAP", "description": "First letter of each word in uppercase", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "STRING", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
        curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -s -X POST "${CATALOG_ROOT_URL}/streams/udfs" -F udfConfig='{"name":"CHAR_LENGTH", "displayName": "CHAR_LENGTH", "description": "String length", "type":"FUNCTION", "argTypes":["STRING"], "returnType": "LONG", "className":"builtin", "builtin":true};type=application/json' -F builtin=true
}

function main {
    echo ""
    echo "===================================================================================="
    echo "Running bootstrap.sh will create streamline default components, notifiers, udfs and roles"
    skip_migration_if_not_needed
    add_all_bundles
    add_roles_and_users
    add_udfs

    echo "Executing ${bootstrap_dir}/bootstrap-notifiers.sh ${CATALOG_ROOT_URL}"
    ${bootstrap_dir}/bootstrap-notifiers.sh ${CATALOG_ROOT_URL}
}

main