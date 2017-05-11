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
bootstrap_dir=$(dirname $0)
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
  if [[ $verbose == "true" ]]
  then
    echo $response
  else
    echo $response | grep -o '"responseMessage":[^"]*"[^"]*"'
  fi
  echo "--------------------------------------"
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

function usage {
  cat <<-EOF
$0 [-vh] [-d bootstrap_dir]
   -v               verbose
   -h               help
   -d bootstrap_dir specify the bootstrap directory
   -s server        the catalog server host
   -p port          the catalog server port
EOF
  exit 0
}

while getopts 'hvd:s:p:' flag; do
  case "${flag}" in
    h) usage ;;
    v) verbose='true' ;;
    d) bootstrap_dir=$OPTARG ;;
    s) host=$OPTARG ;;
    p) port=$OPTARG ;;
    *) error "Unexpected option ${flag}" ;;
  esac
done

#Below command to update storm version will be called by RE script. Need to remove later. Adding now for convenience
update_storm_version_command="$bootstrap_dir/update-storm-version.sh 1.0.2.2.1.0.0-165"
run_cmd $update_storm_version_command

#---------------------------------------------
# Get catalogRootUrl from configuration file
#---------------------------------------------

CONF_READER_MAIN_CLASS=com.hortonworks.streamline.storage.tool.StreamlinePropertiesReader

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
    add_topology_component_bundle /streams/componentbundles/SOURCE ${component_dir}/sources/kafka-source-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SOURCE ${component_dir}/sources/hdfs-source-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SOURCE ${component_dir}/sources/eventhubs-source-topology-component.json
    # === Processor ===
    add_topology_component_bundle /streams/componentbundles/PROCESSOR ${component_dir}/processors/rule-topology-component.json
    add_topology_component_bundle /streams/componentbundles/PROCESSOR ${component_dir}/processors/window-topology-component.json
    add_topology_component_bundle /streams/componentbundles/PROCESSOR ${component_dir}/processors/branch-topology-component.json
    add_topology_component_bundle /streams/componentbundles/PROCESSOR ${component_dir}/processors/join-bolt-topology-component.json
    add_topology_component_bundle /streams/componentbundles/PROCESSOR ${component_dir}/processors/model-topology-component.json
    add_topology_component_bundle /streams/componentbundles/PROCESSOR ${component_dir}/processors/projection-topology-component.json
    # === Sink ===
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/hdfs-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/hbase-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/notification-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/opentsdb-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/jdbc-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/cassandra-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/druid-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/solr-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/kafka-sink-topology-component.json
    add_topology_component_bundle /streams/componentbundles/SINK ${component_dir}/sinks/hive-sink-topology-component.json
    # === Topology ===
    add_topology_component_bundle /streams/componentbundles/TOPOLOGY ${component_dir}/topology/storm-topology-component.json

    #add_topology_component_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/split-topology-component
    #add_topology_component_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/normalization-processor-topology-component.json
    #add_topology_component_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/multilang-topology-component.json
    #post /streams/componentbundles/PROCESSOR $component_dir/sinks/stage-topology-component
    #post /streams/componentbundles/ACTION $component_dir/sinks/transform-action-topology-component
    #post /streams/componentbundles/TRANSFORM $component_dir/sinks/projection-transform-topology-component
    #post /streams/componentbundles/TRANSFORM $component_dir/sinks/enrichment-transform-topology-component
    #note that the below is just a sample for ui to work with. Once UI is ready, all above will be replaced with new bundle components
    #add_sample_bundle

    # === service bundles ===
    post /servicebundles ${service_dir}/zookeeper-bundle.json
    post /servicebundles ${service_dir}/storm-bundle.json
    post /servicebundles ${service_dir}/kafka-bundle.json
    post /servicebundles ${service_dir}/hdfs-bundle.json
    post /servicebundles ${service_dir}/hbase-bundle.json
    post /servicebundles ${service_dir}/hive-bundle.json
    post /servicebundles ${service_dir}/email-bundle.json

    # === anonymous user ===
    post /users ${user_role_dir}/user_anon.json

    # === system roles ===
    for i in ${user_role_dir}/role_*
    do
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

    #----------------------------------
    # Execute other bootstrap scripts
    #----------------------------------
    script_dir=$(dirname $0)
    echo "Executing ${script_dir}/bootstrap-udf.sh ${CATALOG_ROOT_URL}"
    ${script_dir}/bootstrap-udf.sh ${CATALOG_ROOT_URL}

    echo "Executing ${script_dir}/bootstrap-notifiers.sh ${CATALOG_ROOT_URL}"
    ${script_dir}/bootstrap-notifiers.sh ${CATALOG_ROOT_URL}
}

function main {
    echo ""
    echo "===================================================================================="
    echo "Running bootstrap.sh will create streamline default components, notifiers, udfs and roles"
    add_all_bundles
}

main
