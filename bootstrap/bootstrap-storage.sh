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
SCRIPT_ROOT_DIR="${BOOTSTRAP_DIR}/sql"

# Which java to use
if [ -z "${JAVA_HOME}" ]; then
  JAVA="java"
else
  JAVA="${JAVA_HOME}/bin/java"
fi

TABLE_INITIALIZER_MAIN_CLASS=com.hortonworks.streamline.storage.tool.TablesInitializer
for file in "${BOOTSTRAP_DIR}"/lib/*.jar;
do
    CLASSPATH="$CLASSPATH":"$file"
done

echo "Using Configuration file: ${CONFIG_FILE_PATH}"

function dropTables {
    ${JAVA} -Dbootstrap.dir=$BOOTSTRAP_DIR  -cp ${CLASSPATH} ${TABLE_INITIALIZER_MAIN_CLASS} -m ${MYSQL_JAR_URL_PATH} -c ${CONFIG_FILE_PATH} -s ${SCRIPT_ROOT_DIR} --drop
}

function createTables {
    ${JAVA} -Dbootstrap.dir=$BOOTSTRAP_DIR  -cp ${CLASSPATH} ${TABLE_INITIALIZER_MAIN_CLASS} -m ${MYSQL_JAR_URL_PATH} -c ${CONFIG_FILE_PATH} -s ${SCRIPT_ROOT_DIR} --create
}

function checkStorageConnection {
    ${JAVA} -Dbootstrap.dir=$BOOTSTRAP_DIR  -cp ${CLASSPATH} ${TABLE_INITIALIZER_MAIN_CLASS} -m ${MYSQL_JAR_URL_PATH} -c ${CONFIG_FILE_PATH} -s ${SCRIPT_ROOT_DIR} --check-connection
}

function printUsage {
    echo "USAGE: $0 [create|drop|check-connection|drop-create]"
}

opt="create"

if [ $# -gt 0 ]
then
    opt="$1"
fi

case "${opt}" in
create)
    createTables
    ;;
drop)
    dropTables
    ;;
drop-create)
    dropTables && createTables
    ;;
check-connection)
    checkStorageConnection
    if [ $? == 0 ]
    then
        echo "Connection check succeed."
        exit 0
    else
        echo "Connection check failed."
        exit 2
    fi
    ;;
*)
    printUsage
    exit 1
    ;;
esac