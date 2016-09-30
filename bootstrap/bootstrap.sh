#!/usr/bin/env bash 

# defaults
verbose=false
bootstrap_dir=$(dirname $0)
host="localhost"
port="8080"

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
  cmd="curl -sS -X POST http://${host}:${port}/api/v1/catalog$uri --data @$data -H 'Content-Type: application/json'"
  echo "POST $data"
  run_cmd $cmd
}

function add_sample_bundle {
  echo "POST sample_bundle"
  cmd="curl -sS -X POST -i -F topologyComponentBundle=@$bootstrap_dir/kafka-topology-bundle http://${host}:${port}/api/v1/catalog/streams/componentbundles/SOURCE/"
  run_cmd $cmd
}

function add_bundle {
  uri=$1
  data=$2
  cmd="curl -sS -X POST -i -F topologyComponentBundle=@$data http://${host}:${port}/api/v1/catalog$uri"
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


add_bundle /streams/componentbundles/SOURCE $bootstrap_dir/kafka-topology-component
add_bundle /streams/componentbundles/PROCESSOR $bootstrap_dir/rule-topology-component
add_bundle /streams/componentbundles/PROCESSOR $bootstrap_dir/window-topology-component
add_bundle /streams/componentbundles/PROCESSOR $bootstrap_dir/parser-topology-component
add_bundle /streams/componentbundles/PROCESSOR $bootstrap_dir/normalization-processor-topology-component
add_bundle /streams/componentbundles/SINK $bootstrap_dir/hdfs-topology-component
add_bundle /streams/componentbundles/SINK $bootstrap_dir/hbase-topology-component
add_bundle /streams/componentbundles/SINK $bootstrap_dir/notification-topology-component
add_bundle /streams/componentbundles/SINK $bootstrap_dir/opentsdb-sink-topology-component
add_bundle /streams/componentbundles/PROCESSOR $bootstrap_dir/branch-topology-component
add_bundle /streams/componentbundles/PROCESSOR $bootstrap_dir/split-topology-component
add_bundle /streams/componentbundles/PROCESSOR $bootstrap_dir/join-topology-component
#post /streams/componentbundles/PROCESSOR $bootstrap_dir/stage-topology-component
#post /streams/componentbundles/ACTION $bootstrap_dir/transform-action-topology-component
#post /streams/componentbundles/TRANSFORM $bootstrap_dir/projection-transform-topology-component
#post /streams/componentbundles/TRANSFORM $bootstrap_dir/enrichment-transform-topology-component
#note that the below is just a sample for ui to work with. Once UI is ready, all above will be replaced with new bundle components
#add_sample_bundle

#----------------------------------
# Execute other bootstrap scripts
#----------------------------------
script_dir=$(dirname $0)
echo "Executing ${script_dir}/bootstrap-udf.sh ${host} ${port}"
${script_dir}/bootstrap-udf.sh ${host} ${port}

echo "Executing ${script_dir}/bootstrap-notifiers.sh ${host} ${port}"
${script_dir}/bootstrap-notifiers.sh ${host} ${port}

