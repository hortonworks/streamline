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

post /system/componentdefinitions/SOURCE kafka-topology-component
post /system/componentdefinitions/PROCESSOR rule-topology-component
post /system/componentdefinitions/PROCESSOR parser-topology-component
post /system/componentdefinitions/PROCESSOR normalization-processor-topology-component
post /system/componentdefinitions/SINK hdfs-topology-component
post /system/componentdefinitions/SINK hbase-topology-component
post /system/componentdefinitions/SINK notification-topology-component
post /system/componentdefinitions/LINK all-grouping-link-topology-component
post /system/componentdefinitions/LINK direct-grouping-link-topology-component
post /system/componentdefinitions/LINK shuffle-grouping-link-topology-component
post /system/componentdefinitions/LINK local-or-shuffle-grouping-link-topology-component
post /system/componentdefinitions/LINK fields-grouping-link-topology-component
post /system/componentdefinitions/LINK global-grouping-link-topology-component
post /system/componentdefinitions/LINK none-grouping-link-topology-component

