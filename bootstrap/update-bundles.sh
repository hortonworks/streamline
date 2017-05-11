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

function update_bundle {
  uri=$1
  data=$2
  cmd="curl -i --negotiate -u:anyUser  -b /tmp/cookiejar.txt -c /tmp/cookiejar.txt -sS -X PUT -i -F topologyComponentBundle=@$data ${CATALOG_ROOT_URL}$uri"
  echo "PUT $data"
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

echo "Configuration file: ${CONFIG_FILE_PATH}"

CATALOG_ROOT_URL=`exec ${JAVA} -cp ${CLASSPATH} ${CONF_READER_MAIN_CLASS} ${CONFIG_FILE_PATH} ${CATALOG_ROOT_URL_PROPERTY_KEY}`

# if it doesn't exit with code 0, just give up
if [ $? -ne 0 ]; then
  exit 1
fi

echo "Catalog Root URL: ${CATALOG_ROOT_URL}"
echo $component_dir

function update_all_bundles {
    # === Source ===
    update_bundle /streams/componentbundles/SOURCE $component_dir/sources/kafka-source-topology-component.json
    update_bundle /streams/componentbundles/SOURCE $component_dir/sources/hdfs-source-topology-component.json
    # === Processor ===
    update_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/rule-topology-component.json
    update_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/window-topology-component.json
    update_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/branch-topology-component.json
    update_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/join-bolt-topology-component.json
    update_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/model-topology-component.json
    # === Sink ===
    update_bundle /streams/componentbundles/SINK $component_dir/sinks/hdfs-sink-topology-component.json
    update_bundle /streams/componentbundles/SINK $component_dir/sinks/hbase-sink-topology-component.json
    update_bundle /streams/componentbundles/SINK $component_dir/sinks/notification-sink-topology-component.json
    update_bundle /streams/componentbundles/SINK $component_dir/sinks/opentsdb-sink-topology-component.json
    update_bundle /streams/componentbundles/SINK $component_dir/sinks/jdbc-sink-topology-component.json
    update_bundle /streams/componentbundles/SINK $component_dir/sinks/cassandra-sink-topology-component.json
    update_bundle /streams/componentbundles/SINK $component_dir/sinks/druid-sink-topology-component.json
    update_bundle /streams/componentbundles/SINK $component_dir/sinks/solr-sink-topology-component.json
    update_bundle /streams/componentbundles/SINK $component_dir/sinks/kafka-sink-topology-component.json
    # === Topology ===
    update_bundle /streams/componentbundles/TOPOLOGY $component_dir/topology/storm-topology-component.json

    #update_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/split-topology-component
    #update_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/normalization-processor-topology-component.json
    #update_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/multilang-topology-component.json
}

function main {
    echo ""
    echo "===================================================================================="
    echo "Running update_bundles.sh will update all bunles with data from component json files in bootstrap directory."
    update_all_bundles
}

main
