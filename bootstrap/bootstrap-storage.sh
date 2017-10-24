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

TABLE_INITIALIZER_MAIN_CLASS=com.hortonworks.registries.storage.tool.TablesInitializer
for file in "${BOOTSTRAP_DIR}"/lib/*.jar;
do
    CLASSPATH="$CLASSPATH":"$file"
done

function execute {
    echo "Using Configuration file: ${CONFIG_FILE_PATH}"
    ${JAVA} -Dbootstrap.dir=$BOOTSTRAP_DIR  -cp ${CLASSPATH} ${TABLE_INITIALIZER_MAIN_CLASS} -m ${MYSQL_JAR_URL_PATH} -c ${CONFIG_FILE_PATH} -s ${SCRIPT_ROOT_DIR} --${1}
}

function printUsage {
    cat <<-EOF
USAGE: $0 [create|migrate|info|validate|drop|drop-create|repair|check-connection]
   create           : Creates the tables. The target database should be empty
   migrate          : Migrates the database to the latest version or creates the tables if the database is empty. Use "info" to see the current version and the pending migrations
   info             : Shows the list of migrations applied and the pending migration waiting to be applied on the target database
   validate         : Checks if the all the migrations haven been applied on the target database
   drop             : Drops all the tables in the target database
   drop-create      : Drops and recreates all the tables in the target database.
   repair           : Repairs the DATABASE_CHANGE_LOG table which is used to track all the migrations on the target database.
                      This involves removing entries for the failed migrations and update the checksum of migrations already applied on the target databsase.
   check-connection : Checks if a connection can be sucessfully obtained for the target database
EOF
}

if [ $# -gt 1 ]
then
    echo "More than one argument specified, please use only one of the below options"
    printUsage
    exit 1
fi

opt="$1"

case "${opt}" in
create | drop | migrate | info | validate | repair | check-connection )
    execute "${opt}"
    ;;
drop-create )
    execute "drop" && execute "create"
    ;;
*)
    printUsage
    exit 1
    ;;
esac