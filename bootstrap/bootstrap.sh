#!/usr/bin/env bash 

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
  cmd="curl -sS -X POST ${CATALOG_ROOT_URL}$uri --data @$data -H 'Content-Type: application/json'"
  echo "POST $data"
  run_cmd $cmd
}

function add_sample_bundle {
  echo "POST sample_bundle"
  cmd="curl -sS -X POST -i -F topologyComponentBundle=@$bootstrap_dir/kafka-topology-bundle ${CATALOG_ROOT_URL}/streams/componentbundles/SOURCE/"
  run_cmd $cmd
}

function add_bundle {
  uri=$1
  data=$2
  cmd="curl -sS -X POST -i -F topologyComponentBundle=@$data ${CATALOG_ROOT_URL}$uri"
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
CLASSPATH=${bootstrap_dir}/lib/storage-tool-0.1.0-SNAPSHOT.jar:
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

function add_all_bundles {
    # === Source ===
    add_bundle /streams/componentbundles/SOURCE $component_dir/sources/kafka-source-topology-component.json
    add_bundle /streams/componentbundles/SOURCE $component_dir/sources/hdfs-source-topology-component.json
    # === Processor ===
    add_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/rule-topology-component.json
    add_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/window-topology-component.json
    add_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/branch-topology-component.json
    add_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/join-bolt-topology-component.json
    add_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/model-topology-component.json
    # === Sink ===
    add_bundle /streams/componentbundles/SINK $component_dir/sinks/hdfs-sink-topology-component.json
    add_bundle /streams/componentbundles/SINK $component_dir/sinks/hbase-sink-topology-component.json
    add_bundle /streams/componentbundles/SINK $component_dir/sinks/notification-sink-topology-component.json
    add_bundle /streams/componentbundles/SINK $component_dir/sinks/opentsdb-sink-topology-component.json
    add_bundle /streams/componentbundles/SINK $component_dir/sinks/jdbc-sink-topology-component.json
    add_bundle /streams/componentbundles/SINK $component_dir/sinks/cassandra-sink-topology-component.json
    add_bundle /streams/componentbundles/SINK $component_dir/sinks/druid-sink-topology-component.json
    add_bundle /streams/componentbundles/SINK $component_dir/sinks/solr-sink-topology-component.json
    add_bundle /streams/componentbundles/SINK $component_dir/sinks/kafka-sink-topology-component.json
    # === Topology ===
    add_bundle /streams/componentbundles/TOPOLOGY $component_dir/topology/storm-topology-component.json

    #add_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/split-topology-component
    #add_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/normalization-processor-topology-component.json
    #add_bundle /streams/componentbundles/PROCESSOR $component_dir/processors/multilang-topology-component.json
    #post /streams/componentbundles/PROCESSOR $component_dir/sinks/stage-topology-component
    #post /streams/componentbundles/ACTION $component_dir/sinks/transform-action-topology-component
    #post /streams/componentbundles/TRANSFORM $component_dir/sinks/projection-transform-topology-component
    #post /streams/componentbundles/TRANSFORM $component_dir/sinks/enrichment-transform-topology-component
    #note that the below is just a sample for ui to work with. Once UI is ready, all above will be replaced with new bundle components
    #add_sample_bundle

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
    echo "Running bootstrap.sh will create streamline default components. This script should be"
    echo "executed only once. Re-running bootstrap.sh script can create duplicate components."
    read -p "Are you sure you want to proceed. (y/n)? " yesorno
    
    case ${yesorno:0:1} in
        y|Y)
            add_all_bundles;;
        * )
            exit;;
    esac
}

main
