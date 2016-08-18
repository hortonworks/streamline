#!/usr/bin/env bash 

verbose=false
bootstrap_dir=$(dirname $0)

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

function add_nest_parser {
  echo "POST nest_parser"
  cmd="curl -sS -X POST -i -F parserJar=@../parsers/target/parsers-0.1.0-SNAPSHOT.jar -F parserInfo='{\"id\":1, \"name\":\"NestParser\",\"className\":\"com.hortonworks.iotas.parsers.nest.NestParser\",\"version\":0}' -F schemaFromParserJar=true http://localhost:8080/api/v1/catalog/parsers"
  run_cmd $cmd
}

function add_console_custom_processor {
  echo "POST console_custom_processor"
  cmd="curl -sS -X POST -i -F jarFile=@../core/target/core-0.1.0-SNAPSHOT.jar -F imageFile=@../webservice/src/main/resources/assets/libs/bower/jquery-ui/css/images/animated-overlay.gif http://localhost:8080/api/v1/catalog/system/componentdefinitions/PROCESSOR/custom -F customProcessorInfo=@console_custom_processor"
  run_cmd $cmd
}

function post {
  uri=$1
  data=$2
  cmd="curl -sS -X POST http://localhost:8080/api/v1/catalog$uri --data @$data -H 'Content-Type: application/json'"
  echo "POST $data"
  run_cmd $cmd
}

function usage {
  cat <<-EOF 
$0 [-vh] [-d bootstrap_dir]
   -v               verbose
   -h               help
   -d bootstrap_dir specify the bootstrap directory
EOF
  exit 0
}

while getopts 'hvd:' flag; do
  case "${flag}" in
    h) usage ;;
    v) verbose='true' ;;
    d) bootstrap_dir=$OPTARG ;;
    *) error "Unexpected option ${flag}" ;;
  esac
done

post /system/componentdefinitions/SOURCE $bootstrap_dir/kafka-topology-component
post /system/componentdefinitions/PROCESSOR $bootstrap_dir/rule-topology-component
post /system/componentdefinitions/PROCESSOR $bootstrap_dir/parser-topology-component
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
