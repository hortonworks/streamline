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


# Resolve links - $0 may be a softlink
PRG="${0}"

while [ -h "${PRG}" ]; do
  ls=`ls -ld "${PRG}"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "${PRG}"`/"$link"
  fi
done

BOOTSTRAP_DIR=`dirname ${PRG}`
CONFIG_FILE_PATH=${BOOTSTRAP_DIR}/../conf/streamline.yaml
MYSQL_JAR_URL_PATH=https://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-5.1.40.zip

# Which java to use
if [ -z "${JAVA_HOME}" ]; then
  JAVA="java"
else
  JAVA="${JAVA_HOME}/bin/java"
fi

SCRIPT_RUNNER_MAIN_CLASS=com.hortonworks.streamline.storage.tool.SQLScriptRunner
for file in "${BOOTSTRAP_DIR}"/lib/*.jar;
do
    CLASSPATH="$CLASSPATH":"$file"
done

echo "Using Configuration file: ${CONFIG_FILE_PATH}"

function streamlineBootstrapStorage {
    SCRIPT_DIR="${BOOTSTRAP_DIR}/sql/<dbtype>"
    FILE_OPT="-f ${SCRIPT_DIR}/drop_tables.sql -f ${SCRIPT_DIR}/create_tables.sql"

    echo "Script files option: $FILE_OPT"
    exec ${JAVA} -Dstreamline.bootstrap.dir=$BOOTSTRAP_DIR  -cp ${CLASSPATH} ${SCRIPT_RUNNER_MAIN_CLASS} -m ${MYSQL_JAR_URL_PATH} -c ${CONFIG_FILE_PATH} ${FILE_OPT}
}

function main {
    echo ""
    echo "===================================================================================="
    echo "Running bootstrap-storage will drop any existing streamline tables and re-create them."
    read -p "Are you sure you want to proceed. (y/n)? " yesorno
    
    case ${yesorno:0:1} in
        y|Y)
            streamlineBootstrapStorage;;
        * )
            exit;;
    esac
}

main
