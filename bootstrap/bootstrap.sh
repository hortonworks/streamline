#!/usr/bin/env bash 

verbose=false

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
$0 [-vh]
   -v verbose
   -h help
EOF
  exit 0
}

while getopts 'hv' flag; do
  case "${flag}" in
    h) usage ;;
    v) verbose='true' ;;
    *) error "Unexpected option ${flag}" ;;
  esac
done

base_dir=$(dirname $0)
post /system/componentdefinitions/SOURCE $base_dir/kafka-topology-component
post /system/componentdefinitions/PROCESSOR $base_dir/rule-topology-component
post /system/componentdefinitions/PROCESSOR $base_dir/parser-topology-component
post /system/componentdefinitions/PROCESSOR $base_dir/normalization-processor-topology-component
post /system/componentdefinitions/SINK $base_dir/hdfs-topology-component
post /system/componentdefinitions/SINK $base_dir/hbase-topology-component
post /system/componentdefinitions/SINK $base_dir/notification-topology-component
post /system/componentdefinitions/SINK $base_dir/opentsdb-sink-topology-component
post /system/componentdefinitions/LINK $base_dir/all-grouping-link-topology-component
post /system/componentdefinitions/LINK $base_dir/direct-grouping-link-topology-component
post /system/componentdefinitions/LINK $base_dir/shuffle-grouping-link-topology-component
post /system/componentdefinitions/LINK $base_dir/local-or-shuffle-grouping-link-topology-component
post /system/componentdefinitions/LINK $base_dir/fields-grouping-link-topology-component
post /system/componentdefinitions/LINK $base_dir/global-grouping-link-topology-component
post /system/componentdefinitions/LINK $base_dir/none-grouping-link-topology-component
post /system/componentdefinitions/PROCESSOR $base_dir/split-topology-component
post /system/componentdefinitions/PROCESSOR $base_dir/join-topology-component
post /system/componentdefinitions/PROCESSOR $base_dir/stage-topology-component
post /system/componentdefinitions/ACTION $base_dir/transform-action-topology-component
post /system/componentdefinitions/TRANSFORM $base_dir/projection-transform-topology-component
post /system/componentdefinitions/TRANSFORM $base_dir/enrichment-transform-topology-component
