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

post /system/componentdefinitions/SOURCE $bootstrap_dir/kafka-topology-component
post /system/componentdefinitions/PROCESSOR $bootstrap_dir/rule-topology-component
post /system/componentdefinitions/PROCESSOR $bootstrap_dir/branch-topology-component
post /system/componentdefinitions/PROCESSOR $bootstrap_dir/normalization-processor-topology-component
post /system/componentdefinitions/SINK $bootstrap_dir/hdfs-topology-component
post /system/componentdefinitions/SINK $bootstrap_dir/hbase-topology-component
post /system/componentdefinitions/SINK $bootstrap_dir/notification-topology-component
post /system/componentdefinitions/SINK $bootstrap_dir/opentsdb-sink-topology-component
post /system/componentdefinitions/LINK $bootstrap_dir/all-grouping-link-topology-component
post /system/componentdefinitions/LINK $bootstrap_dir/direct-grouping-link-topology-component
post /system/componentdefinitions/LINK $bootstrap_dir/shuffle-grouping-link-topology-component
post /system/componentdefinitions/LINK $bootstrap_dir/local-or-shuffle-grouping-link-topology-component
post /system/componentdefinitions/LINK $bootstrap_dir/fields-grouping-link-topology-component
post /system/componentdefinitions/LINK $bootstrap_dir/global-grouping-link-topology-component
post /system/componentdefinitions/LINK $bootstrap_dir/none-grouping-link-topology-component
post /system/componentdefinitions/PROCESSOR $bootstrap_dir/split-topology-component
post /system/componentdefinitions/PROCESSOR $bootstrap_dir/join-topology-component
post /system/componentdefinitions/PROCESSOR $bootstrap_dir/stage-topology-component
post /system/componentdefinitions/ACTION $bootstrap_dir/transform-action-topology-component
post /system/componentdefinitions/TRANSFORM $bootstrap_dir/projection-transform-topology-component
post /system/componentdefinitions/TRANSFORM $bootstrap_dir/enrichment-transform-topology-component

#----------------------------------
# Execute other bootstrap scripts
#----------------------------------
script_dir=$(dirname $0)
echo "Executing ${script_dir}/bootstrap-udf.sh ${host} ${port}"
${script_dir}/bootstrap-udf.sh ${host} ${port}

echo "Executing ${script_dir}/bootstrap-notifiers.sh ${host} ${port}"
${script_dir}/bootstrap-notifiers.sh ${host} ${port}
